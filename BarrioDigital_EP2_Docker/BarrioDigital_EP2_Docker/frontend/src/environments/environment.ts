type RuntimeConfig={authMode?:'entra'|'demo';apiUrl?:string;clientId?:string;tenantId?:string;backendScope?:string};
const runtime=(window as Window & {__BD_CONFIG__?:RuntimeConfig}).__BD_CONFIG__??{};
const tenant=runtime.tenantId??'f402d005-2163-4d89-a6cd-e07c25b973b2';
export const environment={
  production:true,
  authMode:runtime.authMode??'entra',
  apiUrl:runtime.apiUrl||window.location.origin,
  azure:{clientId:runtime.clientId??'d7f096c4-0ef5-4833-8def-e802ffd7314a',tenantId:tenant,
    authority:`https://login.microsoftonline.com/${tenant}`,redirectUri:window.location.origin+'/',
    backendScope:runtime.backendScope??'api://54bc913a-2f45-4e55-9eae-b30616a9f8d5/OT.Create'}
};
