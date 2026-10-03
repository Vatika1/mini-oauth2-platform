# AI-Assisted Development

I used AI as an engineering assistant rather than relying on it purely for code generation. This file describes what I asked for, what it produced, what I wrote myself, and where I changed or rejected its output.

## Tools

### GitHub Copilot
- IDE autocomplete for repetitive lines such as imports and builder chains
- Small refactors and code-completion suggestions

### Claude Chat
- Reasoning through JWT, JWK/JWKS, `kid`, signing, and verification flows
- Comparing implementation alternatives and trade-offs
- Challenging architecture and security decisions
- Clarifying Spring Security and OAuth/JWT concepts

### Claude Code
- Project-level and cross-file code review
- Debugging failing tests and runtime issues
- Identifying missing negative and edge-case tests
- Tracing authentication and JWT-validation flows across multiple components

## Engineering validation

AI suggestions were treated as proposals, not automatically accepted. I reviewed generated code, validated behavior through tests and Postman, challenged assumptions, and adjusted or rejected suggestions when they did not accurately reflect the intended security behavior or design. The sections below give the specific cases.

## Background

I have built JWT authentication and role-based access with Spring Security before. For this project I used AI to study the specifics of RS256 signing, JWKS and Spring's resource server support before writing code, so that I could explain every line.

## How I worked

For most of the project I asked the AI to describe what a class needed in words, wrote the class myself, and then had it review my code. I ran everything myself, in the IDE and in Postman, before committing.

## How I used it, with examples

### 1. I challenged the AI and overruled it when it was wrong

- **Spring Security in the auth server.** It was on the suggested dependency list. I asked why the auth server needed it. It does not: the auth server only issues tokens and has no protected endpoints. I left it out.
- **A test assertion that did not compile.** It suggested combining two Hamcrest matchers with `allOf(hasSize(2), everyItem(is("RSA")))`. Java could not resolve the generic types. I replaced it with two separate assertions.
- **An unnecessary `throws Exception`.** It said the `SecurityFilterChain` bean method required it. My IDE showed that it did not in this Spring Security version, so I left it off.
- **An outdated dependency name.** It gave the Spring Boot 3 name for the resource server starter. In Spring Boot 4 it is `spring-boot-starter-security-oauth2-resource-server`, which is what my `pom.xml` uses.
- **One shared Dockerfile.** The AI first generated a single Dockerfile for both services, selected by a build argument. I questioned that and used one Dockerfile per service, which is the standard convention and lets each service change independently.
- **A wrong diagnosis of a Docker build failure.** The build could not find the `resource-server` module. The AI said the parent POM did not list it and suggested changing the Dockerfiles to work around it. I checked the parent POM and found the module line was commented out. I uncommented it and kept the original Dockerfiles.

### 2. I verified everything by running it

I tested each endpoint in Postman as I built it. That is how I found a bug in my JWKS endpoint: returning the Nimbus `JWKSet` object directly let Jackson serialize it with the wrong field names (`keyID`, `keyType`) instead of the RFC 7517 names (`kid`, `kty`). A resource server could not have parsed it. I fixed it by returning `toJSONObject()`, and `JwksControllerTest` now checks the field names.

### 3. I wrote the code and used the AI as a reviewer

Examples of review feedback I applied:

- `KeyProvider`: use the key's own ID as the map key so the `kid` is not typed twice, and fail at startup if `active-key-id` does not match a key.
- `TokenControllerTest`: use `eq("alice")` instead of `any()` in the stub, so the test proves the request's `sub` reaches the service.
- `SecurityConfig`: add an explicit `anyRequest().authenticated()` rule.

### 4. I used the AI to find gaps in my tests

- **A test that could pass on an empty result.** In `JwksControllerTest`, `everyItem(is("RSA"))` passes when the list is empty, so the test would not have caught a missing `kty` field. I added a size check for each field.
- **A test that passed for the wrong reason.** My first tampered-signature test returned 401 because of a wrong audience left over from another test, not because of the signature. I changed it so that the signature is the only invalid part.
- **Two tests covering the same case.** My tampered-signature and unknown-`kid` tests both used a `kid` that was not in the JWKS. I changed the tampered test to use a known `kid` with a different signing key, so each test covers a different check.
- **A borderline expiry.** A token that expired 60 seconds ago sits on Spring's default clock-skew limit. I changed the expired-token test to 300 seconds.

### 5. I used the AI to study the details before building

Before writing code, I asked for explanations of asymmetric signing, JWKS, key rotation and the resource-server model, with diagrams of which service calls which. Later I asked for the internals of the Nimbus objects (`RSAKey`, `JWSHeader`, `JWTClaimsSet`, `SignedJWT`) so that I could explain each line of the token code.

### 6. I compared design options

For key distribution, my first idea was a static list of public keys in the resource server, selected by `kid`. I compared it with a single configured key and with a JWKS endpoint, and chose the JWKS endpoint. The reasoning is in the README.

The AI also advised against Spring Authorization Server for this assignment, and I agreed: a small issuer built with Nimbus is easier to read and explain.

## Other uses

- Scoping: separating what the assignment asks for from what is out of scope.
- Planning: breaking the work into ordered steps, one commit each.
- Test design: checking my list of test cases for missing negative and security cases.
- Debugging: I pasted real stack traces and output. Examples: a port conflict with a Docker container, a missing dependency version in the POM, and a 401 response that was hiding a `NullPointerException`.
- Understanding framework behaviour: for example, why `/hello` returned 401 before I had written any security configuration.
- Documentation: the AI drafted the README from the finished code. I rewrote the key-distribution section from my own notes.
- Commit messages: suggested by the AI, edited where I would name things differently.

## What the AI generated and what I wrote

| Part | Who wrote it |
|---|---|
| Auth server classes (`SigningProperties`, `KeyProvider`, `TokenService`, controllers, `GlobalExceptionHandler`) | Written by me from a description, reviewed by the AI |
| Auth server tests | Written by me. The AI gave the method signatures and reviewed each test |
| `SecurityConfig`, `JsonAuthenticationEntryPoint` | Written by me from a description, reviewed by the AI |
| `HelloController` | The AI gave the three-line method |
| Resource server `application.yaml` | Generated by the AI, checked against the auth server's issuer and audience |
| `HelloSecurityTest` setup (MockWebServer, dispatcher, `@DynamicPropertySource`) | Generated by the AI, reviewed and run by me |
| `HelloSecurityTest` helpers and the eight test cases | Written by me, reviewed by the AI |
| Dockerfiles and `docker-compose.yml` | Generated by the AI, tested by me with `docker compose up --build` |
| README | Drafted by the AI, edited by me |

## Example prompts

- "Describe what this class needs in words. Do not give me the code. I will write it and you review it."
- "I am considering a static list of public keys in the resource server, selected by `kid`. How does that compare with a JWKS endpoint?"
- "Which auto-configuration is protecting `/hello`? I have not defined a `SecurityFilterChain` yet."
- "Show me the internals of each object: the RSA key, the claims set, the header and the signed JWT."
- "Before I commit, review this test class."
