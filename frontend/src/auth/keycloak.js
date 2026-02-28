import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:8180',
  realm: 'manajemen-distrik',
  clientId: 'manajemen-distrik-app',
});

export default keycloak;
