# proof_bus
Proof IT task

#### Implementation description
Simple REST approach: Controller receive HTTP request and pass it to appropriate service. TaxServiceAccessor may not receive data from remote service so possible problematic method "getCurrentVAT" wrapped by circuit breaker. BasePriceRepository could not contain date for particular route name so it returns Optional.

#### Build
`gradlew classes`
#### Run
`gradlew bootRun`
#### Run JUnit tests
`gradlew clean test`
#### Try manually
You can run scripts manually from  `src/test/resources/*.http`
#### Try with UI
Use UI by SpringFox to run it in browser [http://localhost:8765/swagger-ui.html](http://localhost:8765/swagger-ui.html)

