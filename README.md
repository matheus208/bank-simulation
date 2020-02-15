#Transaction Authoriser

##Running
Compile with
    
    mvn clean package
    
Run with

    java -jar target/kotlin-authoriser-1.0-SNAPSHOT.jar  < operations
    
Or build the docker image

    docker build -t kotlin-authoriser . 
    -t tags the image as `kotlin-authoriser`

Run the docker image

    docker run -a stdin -i -a stdout kotlin-authoriser < operations
    
    -a to attach stdin and stdout in the container
    -i to keep interactive mode to be able to redirect the contents of the file to stdin

Run tests with
   
    mvn test

##Architecture
I decided to use an adapted MVC-like architecture. 
- The `App` class acts as a Controller, receiving input, creating the Account, using the `
Authoriser` to handle the transactions business logic and printing the results. 
- The `Authoriser` class handles the business logic, as well as the `EventFrequencyAnalyzer` class, 
which is used in a couple of the rules. 
- Classes under the `models` package are objects representing the data handled by the application.
    - `Account`: represents the account
    - `Transaction`: represents the transaction to be analyzed
    - `Violation`: Enum representing the possible outcomes of any operation
    - `Operation`: Represents a possible operation from the stdin
    - `OperationResult`: Represents the outcome of analyzing an operation. It is composed of the 
    updated Account, plus any violations found.

##Decisions
- I was inspired by the functional paradigm to code this, as I believe it offers some helpful 
features such as immutability (transactions can never change), minimal state, which makes it easy 
to parallelize tasks (multiple transactions can be analyzed at the "same time"), event-sourcing 
helps analyze all transaction history, and so on. However, I have never had any experience with 
coding with it, so I stuck to Kotlin and tried to apply as many concepts as possible.

- I also used TDD for this, that has helped me to design the architecture, and guarantees that 
changes wouldn't break any logic. That was invaluable during the development of the assignment.

- I decided to use an event-sourcing of sorts on the `Authoriser` class, to keep track of all 
valid transactions. Even thought that inserted state to the class, I thought it would 
make it easier to extend the code for any future rules, as described on the requirements.

- The algorithm used to solve the high frequency and "duplicated" transactions was developed after
a brief research online about circular buffers, then I adapted it to fit the requirements. It
might not be optimal, but I'm happy to use it since it's O(n). Also, the frequency and time frame
parameters are completely customizable. That way, any change in business requirements are easy to 
implement.


##TODO list

This is a list of improvements I would have done if I had more time.

- Ideally, `App` should just initialize the application, and deal with the input/output. Any other 
business logic (creating an account, routing account and transaction statements) would be performed 
by a "Services" class. However, for the purposes of this exercise, I thought it would be a lot of
boilerplate code, so I kept it simple.
- Make the `Authoriser` class stateless by refactoring out the `transactions` list to the "Service" 
layer. That way, only the service would have state. 
- Remove the mutability of the account property on the `App` class
- Try to optimize the algorithms used for `high-frequency-small-period` and `doubled-transaction`, 
I feel like the high frequency one might be optimal, at O(n),but the double transaction can probably
be tweaked, I ran out of ideas and transformed it into the same problem, and used the 
EventFrequencyAnalyzer algorithm to make it work.
- Create integration tests (inputting operations on stdin in a test and checking the results), 
using the service layer would be necessary, as I think mocking the stdin/stdout would not be 
elegant.
- Improve the serialization model (Account and Transactions could both implement Operation, that way
I wouldn't have to route between operations by comparing the fields in the current Operation class.)
- The list of rules will grow (as stated in the requirements), so I would think of a way of chaining
these rules. Perhaps also extracting them as an interface, to make it easier to implement and define
what a validation needs to have (receive an account, a list of previous transactions, and an 
analyzed transaction, returning any possible violations).
- Improve Dockerfile by not requiring Maven/existing Jar to build the image.
