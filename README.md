## How to run per environment

### Local dev

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

### Prod-like locally (lambda tools)

```bash
SPRING_PROFILES_ACTIVE=prod AWS_PROFILE=personal AWS_REGION=eu-central-1 ./gradlew bootRun
```

### Tests

Spring Boot automatically sets `test` profile in many setups, but you can force:

```bash
SPRING_PROFILES_ACTIVE=test ./gradlew test
```


## CDK environments (dev/test/prod)

In CDK, you’ll typically do:

* separate stacks per env (`LendingStack-dev`, `LendingStack-prod`)
* env vars injected into lambdas (table names, log retention, etc.)
* names include stage suffix

Example approach:

* functionName: `loan-updateBusinessDetails-dev`
* in Spring config for prod: map tools → prod names

Or: keep stable names in prod only, suffix in dev.

We’ll decide once you know how your company does it, but the structure above supports either.


## How to deploy lambda functions

./gradlew :lambda-tools:shadowJar
cd infra
cdk deploy --profile personal --region eu-central-1
