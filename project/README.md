KTH V17P01
Due 17.03.12

Distributed Systems Advanced Course

Project


Fannar Magnusson (fannar@kth.se)

Thorsteinn Thorri Sigurdsson (ttsi@kth.se)

The project can be built and run using the commands given below, or it can be imported into IntelliJ where the run configurations for the client and servers are included.

### Building

Build the project with

```
maven clean install
```

### Running

#### Bootstrap Server Node
To run a bootstrap server node `cd` into the `server` directory and execute:

```
java -jar target/project17-server-1.0-SNAPSHOT-shaded.jar -p 45678
```

This will start the bootstrap server on localhost:45678.

#### Normal Server Node
After you started a bootstrap server on `<bsip>:<bsport>`, again from the `server` directory execute:

```
java -jar target/project17-server-1.0-SNAPSHOT-shaded.jar -p 56789 -c <bsip>:<bsport>
```
This will start the bootstrap server on localhost:56789, and ask it to connect to the bootstrap server at `<bsip>:<bsport>`.
Make sure you start every node on a different port if they are all running directly on the local machine.

By default you need 6 nodes (including the bootstrap server), before the system will actually generate a lookup table and allow you to interact with it.

#### Clients
To start a client (after the cluster is properly running), `cd` into the `client` directory and execute:

```
java -jar target/project17-client-1.0-SNAPSHOT-shaded.jar -p 56787 -b <bsip>:<bsport>
```

Again, make sure not to double allocate ports on the same machine.

The client will attempt to contact the bootstrap server and give you a small command promt if successful. Type `help` to see the available commands.


