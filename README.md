# grpc-streaming-spike

This is a spike that validates the following.

1. Run an app with user business logic without any direct middleware interactions.
2. Run another app that has all the intelligence around various middleware technologies such as Apache Kafka, RabbitMQ etc.

The above two applications communicate over a commonly agreed upon protocol.
For this spike, we use gRPC as that communication layer between the two. 

## uppercase-grpc-demo

This is the first app that contains the user business logic.
In this case, it simply contains a function that uppercase each String that it receives.
By including the `spring-cloud-function-grpc` module in the classpath of this applicaiton, we essentially make this app an gRPC server component. 

## multibinder-grpc-demo

This is the second app that knows how to communicate to various middleware systems.
This is multi binder app that has both Spring Cloud Stream Kafka and Rabbit binders in it's classpath.
More specifically, this app variant is hard-wired to consume data from a Kafka topic and then post this data through an gRPC request connection to the server in application 1. 
Once the application returns the data (uppercased String), then we will take that response and publish to a RabbitMQ exchange.

## Running the apps locally

1. Start Kafka locally
2. Start RabbitMQ locally
3. Build both apps (`./mvnw clean package`)
4. `java -jar uppercase-grpc-demo/target/uppercase-grpc-demo-0.0.1-SNAPSHOT.jar --spring.grpc.server.port=7000`
5. `java -jar multibinder-grpc-demo/target/multibinder-grpc-demo-0.0.1-SNAPSHOT.jar`
6. Publish some data to the Kafka topic `dataIn`.
7. Receive the data uppercased through the RabbitMQ exchange `dataOut`.

## Running the demo on Kubernetes

The following instructions are for running this demo on minikube. 
If you are using a different cluster, please update the following instructions accordingly. 

Start minikube.

### Infrastructure components:

`cd k8s-templates`

#### Kafka

```
kubectl apply -f kafka/
```
#### RabbitMQ

```
kubectl apply -f rabbitmq/
```

Verify that both Apache Kafka and RabbitMQ are up and running:

`kubectl get all`

You should see something like below:

```
NAME                                   READY   STATUS    RESTARTS   AGE
pod/kafka-broker-68cf7d7847-s95qk      1/1     Running   0          9s
pod/kafka-zk-5cdd8b4c75-txw4h          1/1     Running   0          9s
pod/rabbitmq-broker-7bcff9f86d-6cb7x   1/1     Running   0          2s

NAME                 TYPE        CLUSTER-IP       EXTERNAL-IP   PORT(S)                      AGE
service/kafka        ClusterIP   10.111.162.192   <none>        9092/TCP                     9s
service/kafka-zk     ClusterIP   10.106.10.247    <none>        2181/TCP,2888/TCP,3888/TCP   9s
service/kubernetes   ClusterIP   10.96.0.1        <none>        443/TCP                      45s
service/rabbitmq     ClusterIP   10.102.242.25    <none>        5672/TCP,15672/TCP           2s

NAME                              READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/kafka-broker      1/1     1            1           9s
deployment.apps/kafka-zk          1/1     1            1           9s
deployment.apps/rabbitmq-broker   1/1     1            1           2s

NAME                                         DESIRED   CURRENT   READY   AGE
replicaset.apps/kafka-broker-68cf7d7847      1         1         1       9s
replicaset.apps/kafka-zk-5cdd8b4c75          1         1         1       9s
replicaset.apps/rabbitmq-broker-7bcff9f86d   1         1         1       2s
```

Port forward from Rabbit MQ management service in order to access the Rabbit management UI.

```
kubectl port-forward svc/rabbitmq 15673:15672
```

In this case, we are making the Rabbit management console available on localhost at port `15673`.
Make sure that you can login at `http://localhost:15673`.

### Application containers

Both of these applications are available as docker images on Docker Hub.
We will run the multibinder-grpc app as a sidecar. 

#### Start the deployment of the pod which has function app with multi-binder as sidecar

```
kubectl apply -f app/
```

This step will create a deployment with the pod which has the `uppercase-grpc` as the main container while the `multibinder-grpc` as the sidecar container.

To view this deployment:

```
kubectl get all -l app=uppercase-with-multibinder-grpc
```

```
NAME                                                     READY   STATUS    RESTARTS   AGE
pod/uppercase-with-multibinder-grpc-664d9dc56-r2z4z   2/2     Running   0          53s

NAME                                                 READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/uppercase-with-multibinder-grpc   1/1     1            1           53s

NAME                                                           DESIRED   CURRENT   READY   AGE
replicaset.apps/uppercase-with-multibinder-grpc-664d9dc56   1         1         1       53s
```

This step would have also created a service which is used to expose the `multibinder-grpc` sidecar container.

```
kubectl get svc,endpoints -l app=multibinder-grpc 
```

```
NAME                          TYPE           CLUSTER-IP       EXTERNAL-IP   PORT(S)        AGE
service/multibinder-grpc   LoadBalancer   10.102.154.134   <pending>     80:32143/TCP   5m22s

NAME                            ENDPOINTS         AGE
endpoints/multibinder-grpc   172.17.0.6:8080   5m22s
```

### Verifying the uppercase demo 

As indicated above, our purpose is to verify the following using the regular uppercase function:

Kafka Topic -> Request to gRPC Server -> User function -> Response from gRPC server -> RabbitMQ Exchange

This can be validated by sending some data to the Kafka topic and make sure that we receive the output throuth the Rabbit exchange.

Open an SSH connection to the Kafka broker:

```
kubectl exec -it <kafka-broker-pod-name> -- /bin/bash
```
Update the Kafka broker pod name when you run this.

```
/opt/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic dataIn
```
At the prompt, enter some text.

On your localhost, go to `http://localhost:15763` 

Create a queue binding for the exchange `dataOut` and retrieve messages from the queue.
The text that you entered into the Kafka topic should be received as uppercased through the Rabbit queue.

If you see the data through the queue, then we validated the scenario.

If you don't see the data, chances are that some configuration might be missing. Happy debugging!

### Multibinder App Actuator endpoints

We can find more details about the multibinder app using the various actuator endpoints.

Following are some examples:

Please update the proper external IP for the multibinder app POD (if you are using loadbalancer on minikube).
If you are port forwarding to localhost, then use the appropriate ports on localhost for these.

```
curl 192.168.99.96/actuator/health | jq .

curl 192.168.99.96/actuator/bindings | jq .

curl 192.168.99.96/actuator/metrics | jq .

curl 192.168.99.96/actuator/configprops | jq .
```

** Using the bindings endpoint, we can pause the processing temporarily if we need to throttle or rate limit the incoming traffic.

** Similarly the metrics endpoint will list all the available metrics. You can drill down into each of them further.  

Proper IP or port-forwarding need to be used for the second multibinder sidecar in the aggregator pod.

### Teardown the components

```
kubectl delete pod,deployment,rc,service -l type="streaming-spike"
```
