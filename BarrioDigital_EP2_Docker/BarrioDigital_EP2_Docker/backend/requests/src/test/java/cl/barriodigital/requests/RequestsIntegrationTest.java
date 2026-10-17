package cl.barriodigital.requests;
import cl.barriodigital.requests.repository.TramiteRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.auth.mode=demo","app.outbox.enabled=false","spring.rabbitmq.dynamic=false",
  "spring.datasource.url=jdbc:h2:mem:requests_test;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
class RequestsIntegrationTest {
  @Autowired MockMvc mvc;@Autowired OutboxRepository outbox;@Autowired TramiteRepository repo;@Autowired ObjectMapper mapper;
  @MockitoBean CatalogClient catalog;
  @BeforeEach void setup(){repo.deleteAll();outbox.deleteAll();when(catalog.get("luminarias")).thenReturn(new CatalogClient.Tipo("luminarias","Luminarias","Descripción",1,true));}
  private String create(String user)throws Exception{
    return mapper.readTree(mvc.perform(post("/api/requests").header("X-Demo-Role","Vecino").header("X-Demo-User",user)
      .contentType("application/json").content("{\"tipoId\":\"luminarias\",\"asunto\":\"Foco apagado\",\"descripcion\":\"Poste de luz sin iluminación\",\"clienteEmail\":\"vecino@example.test\",\"propietarioId\":\"suplantado\"}"))
      .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString()).get("id").asString();
  }
  @Test void creationPersistsThreeEventsAndUsesAuthenticatedOwner()throws Exception{
    String id=create("vecino-a");assertThat(repo.findById(id).orElseThrow().getPropietarioId()).isEqualTo("vecino-a");
    assertThat(outbox.count()).isEqualTo(3);assertThat(outbox.findAll()).extracting(e->e.routingKey).containsExactlyInAnyOrder("email.send","tramite.audit.creado","tramite.estado.creado");
  }
  @Test void vecinoCannotReadAnotherOwnersRecordOrChangeStatus()throws Exception{
    String id=create("vecino-a");
    mvc.perform(get("/api/requests/"+id).header("X-Demo-Role","Vecino").header("X-Demo-User","vecino-b")).andExpect(status().isForbidden());
    mvc.perform(get("/api/requests").header("X-Demo-Role","Vecino").header("X-Demo-User","vecino-b")).andExpect(content().json("[]"));
    mvc.perform(put("/api/requests/"+id+"/estado").header("X-Demo-Role","Vecino").header("X-Demo-User","vecino-a").contentType("application/json").content("{\"estado\":\"ADMITIDO\"}")).andExpect(status().isForbidden());
  }
  @Test void invalidInputDoesNotCreateRequestOrEvent()throws Exception{
    mvc.perform(post("/api/requests").header("X-Demo-Role","Vecino").contentType("application/json").content("{\"tipoId\":\"luminarias\",\"asunto\":\"x\",\"descripcion\":\"x\",\"clienteEmail\":\"bad\"}")).andExpect(status().isBadRequest());
    assertThat(repo.count()).isZero();assertThat(outbox.count()).isZero();
  }
  @Test void stateTransitionsEnforceDailyCapacity()throws Exception{
    String one=create("vecino-a"),two=create("vecino-b");
    mvc.perform(put("/api/requests/"+one+"/estado").header("X-Demo-Role","Funcionario").contentType("application/json").content("{\"estado\":\"ADMITIDO\"}")).andExpect(status().isOk());
    mvc.perform(put("/api/requests/"+two+"/estado").header("X-Demo-Role","Funcionario").contentType("application/json").content("{\"estado\":\"ADMITIDO\"}")).andExpect(status().isConflict());
    assertThat(outbox.count()).isEqualTo(9);
  }
  @Test void editAndDeleteArePersisted()throws Exception{
    String id=create("vecino-a");
    mvc.perform(put("/api/requests/"+id).header("X-Demo-Role","Vecino").header("X-Demo-User","vecino-a").contentType("application/json").content("{\"asunto\":\"Luminaria caída\",\"descripcion\":\"Detalle editado en el servidor\"}")).andExpect(status().isOk());
    assertThat(repo.findById(id).orElseThrow().getAsunto()).isEqualTo("Luminaria caída");
    mvc.perform(delete("/api/requests/"+id).header("X-Demo-Role","Vecino").header("X-Demo-User","vecino-a")).andExpect(status().isNoContent());
    assertThat(repo.existsById(id)).isFalse();
  }
}
