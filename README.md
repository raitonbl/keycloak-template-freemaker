# keycloak template freeMaker
Intended to extend [Keycloak](https://www.keycloak.org/) features through Apache FreeMaker provider. This project implements [Keycloak template api](https://github.com/raitonbl/keycloak-template)

## Building

Ensure you have JDK 8 (or newer), Maven 3.5.4 (or newer) and Git installed

    java -version
    mvn -version
    git --version

How to build:

    mvn clean package

## Deployment    

In order to deploy the implementation , Keycloak must be stopped and the generated jar should be deployed on **KEYCLOAK_HOME/providers/** (for containers) or on **KEYCLOAK_HOME/standalone/deployments/**.
Its important that [API](https://github.com/raitonbl/keycloak-template) version matches the plugin version and is also deployed on Keycloak.

Start **Keycloak** , [More details](https://www.keycloak.org/documentation.html)

