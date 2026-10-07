---
name: create-rest-endpoint
description: Create or modify a REST endpoint in the ProTube Spring Boot backend following the project's architecture and testing conventions.
---

# Create REST Endpoint

Use this skill when implementing a new REST endpoint in the ProTube backend.

## Steps

1. Check REQUIREMENT.md for the relevant user story, acceptance criteria, and priority.

2. Inspect existing backend code and follow the conventions already used by the project.

3. Create or update the required DTOs.
    - Controllers must receive and return DTOs.
    - Do not expose JPA entities directly.

4. Implement the required layers:
    - Controller: HTTP request/response handling.
    - Service: business logic.
    - Repository: persistence access when required.

5. Keep the dependency direction:
   Controller -> Service -> Repository.

6. Add or update tests for the new behaviour.

7. Run the backend verification from backend/:

   mvn clean verify

8. If coverage needs to be checked, run:

   mvn clean verify -Pcoverage

9. Ensure the change satisfies the project's Definition of Done in AGENTS.md.

## Important

- Do not hardcode local paths or environment-specific values.
- Do not bypass the service layer from controllers.
- Do not change unrelated code.
- Follow existing naming and package conventions.