# rsocket-streaming-spike

This is a spike that validates the following.

1. Run an app with user business logic without any direct middleware interactions.
2. Run another app that has all the intelligence around various middleware technologies such as Apache Kafka, RabbitMQ etc.

The above two applications communicate over a commonly agreed upon protocol.
For this spike, we use RSocket as that communication layer between the two. 

## uppercase-rsocket-demo

This is the first app that contains the user business logic.
In this case, it simply contains a function that uppercase each String that it receives.
By including the `spring-cloud-function-rsocket` module in the classpath of this applicaiton, we essentially make this app an RSocket server component. 

## multibinder-rsocket-demo

This is the second app that knows how to communicate to various middleware systems.
This is multi binder app that has both Spring Cloud Stream Kafka and Rabbit binders in it's classpath.
More specifically, this app variant is hard-wired to consume data from a Kafka topic and then post this data through an RSocket request connection to the server in application 1. 
Once the application returns the data (uppercased String), then we will take that response and publish to a RabbitMQ exchange.

## Running the apps locally

1. Start Kafka locally
2. Start RabbitMQ locally
3. Build both apps (`./mvnw clean package`)
4. `java -jar uppercase-rsocket-demo/target/uppercase-rsocket-demo-0.0.1-SNAPSHOT.jar --spring.rsocket.server.port=7000`
5. `java -jar multibinder-rsocket-demo/target/multibinder-rsocket-demo-0.0.1-SNAPSHOT.jar`
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
kubectl create -f kafka-zk-deployment.yaml
kubectl create -f kafka-zk-svc.yaml

kubectl create -f kafka-deployment.yaml
kubectl create -f kafka-svc.yaml
```
#### RabbitMQ

Start RabbitMQ using Bitnami Helm charts:

If you don't have the bitnami repo, add this: `helm repo add bitnami https://charts.bitnami.com/bitnami`. 

`helm install bitnami-rabbitmq bitnami/rabbitmq`

Verify that both Apache Kafka and RabbitMQ are up and running:

`kubectl get all`

You should see something like below:

```
NAME                                       READY   STATUS    RESTARTS   AGE
pod/bitnami-rabbitmq-0                     1/1     Running   0          77m
pod/kafka-broker-68cf7d7847-8ll4f          1/1     Running   0          168m
pod/kafka-zk-5cdd8b4c75-nz7vk              1/1     Running   0          168m
```

While starting the Bitnamit RabbitMQ chart, make a note of all the details, especially the way to extract the user credentials for Rabbit. 
Later on, you will need this to connect to the Rabbit environment.

Port forward from Rabbit MQ management service in order to access the Rabbit management UI.

`kubectl port-forward --namespace default svc/bitnami-rabbitmq-0 15673:15672`

In this case, we are making the Rabbit management console available on localhost at port 15673.
Make sure that you can login at `localhost:15673`.

### Application containers

Ideally, we would like to run both apps as two containers on the same pod (side-car patter), but for this demo, we are still going to run them separate as two different pods.
In the next iteration, we will convert this part of the demo to use the sidecar pattern.

Both of these applications are available as docker images on Docker Hub.,

#### Start the uppercase function app

```
kubectl cretae -f uppercase-rsocket.yaml
kubectl cretae -f uppercase-rsocket-svc.yaml
```

#### Start the multibinder app

Before starting the multibinder app, please fill in the rabbit connection credentials in multibinder-rsocket.yaml.
They are left as blank. Refer above for extracting the credentials for the Bitnami RabbitMQ chart.

Also note that the multibinder app uses the `LoadBalancer` type. 
Minikube does not support the `LoadBalancer` type by default. 
Use something like MetalB for that purpose or port forward to your localhost for accessing the binder actuator endpoints.

```
kubectl create -f multibinder-rsocket.yaml
kubectl create -f multibider-rsocket-svc.yaml
```

At this point, all of the components must be up and running. 
It should as below on `kubectl get all`

```
k get all
NAME                                       READY   STATUS    RESTARTS   AGE
pod/bitnami-rabbitmq-0                     1/1     Running   0          77m
pod/kafka-broker-68cf7d7847-8ll4f          1/1     Running   0          168m
pod/kafka-zk-5cdd8b4c75-nz7vk              1/1     Running   0          168m
pod/multibinder-rsocket-64d97c957d-n4hqc   1/1     Running   0          63m
pod/uppercase-rsocket-646ddb7547-bpmfj     1/1     Running   0          136m

NAME                                  TYPE           CLUSTER-IP       EXTERNAL-IP     PORT(S)                                 AGE
service/bitnami-rabbitmq-0            ClusterIP      10.98.200.55     <none>          5672/TCP,4369/TCP,25672/TCP,15672/TCP   77m
service/bitnami-rabbitmq-0-headless   ClusterIP      None             <none>          4369/TCP,5672/TCP,25672/TCP,15672/TCP   77m
service/kafka                         ClusterIP      10.96.214.6      <none>          9092/TCP                                168m
service/kafka-zk                      ClusterIP      10.106.240.127   <none>          2181/TCP,2888/TCP,3888/TCP              168m
service/kubernetes                    ClusterIP      10.96.0.1        <none>          443/TCP                                 237d
service/multibinder-rsocket           LoadBalancer   10.101.161.53    192.168.99.96   80:30816/TCP                            63m
service/uppercase-rsocket             ClusterIP      10.109.166.32    <none>          7000/TCP                                136m

NAME                                  READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/kafka-broker          1/1     1            1           168m
deployment.apps/kafka-zk              1/1     1            1           168m
deployment.apps/multibinder-rsocket   1/1     1            1           63m
deployment.apps/uppercase-rsocket     1/1     1            1           136m

NAME                                             DESIRED   CURRENT   READY   AGE
replicaset.apps/kafka-broker-68cf7d7847          1         1         1       168m
replicaset.apps/kafka-zk-5cdd8b4c75              1         1         1       168m
replicaset.apps/multibinder-rsocket-64d97c957d   1         1         1       63m
replicaset.apps/uppercase-rsocket-646ddb7547     1         1         1       136m

NAME                                  READY   AGE
statefulset.apps/bitnami-rabbitmq-0   1/1     77m
```

### Verifying the demo 

As indicated above, our purpose is to verify the following:

Kafka Topic -> Request to RSocket Server -> User function -> Response from Rsocket server -> RabbitMQ Exchange

This can be validated by sending some data to the Kafka topic and make sure that we receive the output throuth the Rabbit exchange.

Open an SSH connection to the Kafka broker:

```
k exec -it kafka-broker-68cf7d7847-8ll4f -- /bin/bash
```
Update the Kafka broker pod name when you run this.

```
/opt/kafka/bin/kafka-console-producer.sh --broker-list localhost:9092 --topic dataIn
```
At the prompt, enter some text.

On your localhost, go to `http://localhost:15763` (Use the credentials from above)

Create a queue binding for the exchange `dataOut` and retrieve messages from the queue.
THe text that you entered into the Kafka topic should be received as uppercased through the Rabbit queue.

If you see the data through the topic, then we validated the scenario.

If you don't see the data, chances are that some configuration might be missing. Happy debugging!

### Teardown the components

```
kubectl delete pod,deployment,rc,service -l type="streaming-spike"
```

Manually delete all the Bitnami RabbitMQ components.
