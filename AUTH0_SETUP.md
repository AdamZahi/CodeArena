# Auth0 Setup Guide

Use this checklist to configure Auth0 for Code Arena.

## 1) Create the SPA Application
- Go to Auth0 Dashboard > Applications > Create Application
- Type: Single Page Application
- Copy the Client ID

## 2) Create the API
- Go to Auth0 Dashboard > Applications > APIs > Create API
- Name: Code Arena API
- Identifier (Audience): https://codearena.com/api
- Keep signing algorithm as RS256

## 3) Enable Social Connections
- Go to Authentication > Social
- Enable Google and GitHub
- Assign both connections to your SPA Application

## 4) Create Roles
- Go to User Management > Roles
- Create roles: ADMIN, PARTICIPANT, COACH

## 5) Default Role Assignment (Post-Registration Action)
- Go to Actions > Flows > Post User Registration
- Add an Action and paste:

```javascript
exports.onExecutePostUserRegistration = async (event, api) => {
  const { ManagementClient } = require('auth0');
  const client = new ManagementClient({
    domain: event.secrets.DOMAIN,
    clientId: event.secrets.CLIENT_ID,
    clientSecret: event.secrets.CLIENT_SECRET
  });

  await client.assignRolestoUser(
    { id: event.user.user_id },
    { roles: ['PARTICIPANT_ROLE_ID'] }
  );
};
```

- Add Action secrets:
  - DOMAIN
  - CLIENT_ID
  - CLIENT_SECRET

## 6) Add Roles to Tokens (Post-Login Action)
- Go to Actions > Flows > Login
- Add an Action and paste:

```javascript
exports.onExecutePostLogin = async (event, api) => {
  const namespace = 'https://codearena.com/roles';
  const roles = event.authorization?.roles ?? [];
  api.idToken.setCustomClaim(namespace, roles);
  api.accessToken.setCustomClaim(namespace, roles);
};
```

## 7) Configure Allowed URLs
- Application Settings:
  - Allowed Callback URLs: http://localhost:4200
  - Allowed Logout URLs: http://localhost:4200
  - Allowed Web Origins: http://localhost:4200

## 8) Environment Variables
Populate `.env` (see `.env.example`) with:
- AUTH0_DOMAIN
- AUTH0_CLIENT_ID
- AUTH0_CLIENT_SECRET
- AUTH0_AUDIENCE
