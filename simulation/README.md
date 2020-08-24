# Simulation

Um das Zusammenspiel zwischen Producer und Consumer zu simulieren, werden eine Reihe von Java EE Anwendungen in Docker Containern genutzt. 
Die Nutzung von Docker Container erleichtert das Starten und Stoppen mehrere Instanzen der selben Anwendungen (z.B. mehrere Consumer) sowie des MQ Brokers.  


## Simulation Producer & Consumer 

Die beiden Integration-Test `ProducerConsumerIT` demonstrieren eine einfache Kommunikation zwischen einen Producer (`QueueProducer` bzw 
`TopicProducer`) und einen Consumer (`QueueConsumer` bzw. `TopicConsumer`) über ein queue- bzw. topic-basiertes Pub/Sub System. Mit dem 
Test soll die Funktionsfähigkeit des Pub/Sub Systems demonstriert werden.  


## Simulation DLQ Consumer

Die beiden Integration-Test `DLQConsumerIT` demonstrieren eine einfache Kommunikation zwischen einen Producer (`QueueProducer` bzw 
`TopicProducer`) und einen Consumer (`QueueConsumer` bzw. `TopicConsumer`) über ein queue- bzw. topic-basiertes Pub/Sub System, bei der die
verschickte Nachricht durch den Subscriber nicht erfolgreich verarbeitet und somit nicht zugestellt werden kann. Die Nachricht wird nach 
erfolglosen Zustellung in die Dead Letter Queue verschoben und durch den DLQConsumer verarbeitet. Mit dem Test soll die Funktionsfähigkeit 
des Pub/Sub Systems inkl. Fehlerhandling demonstriert werden. 


## Simulation Pub/Sub mit mehreren Consumern

Die beiden Integration-Test `PubSubSimulationIT` demonstrieren die Kommunikation zwischen einen Producer (`QueueProducer` bzw 
`TopicProducer`) und mehreren Consumer (`QueueConsumer` bzw. `TopicConsumer`,`DurableTopicConsumer`,`SharedTopicConsumer`) über ein queue- 
bzw. topic-basiertes Pub/Sub System. 


### Simulation Pub/Sub mit Queue(s)

![simulation-queue](../doc/images/simulationQueue.png)

Das queue-basierte Pub/Sub System für die Simulation verfügt über einen Producer und drei Consumer. Die Abbildung zeigt die Konsumierung 
von sechs _Messages_ die in der Simulation versendet werden. 

_Consumer A_ ist zum Sendezeitpunkt aller Messages 'online' und konsumiert die Nachrichten sobald diese gesendet wurden. _Consumer B_ und 
_Consumer C_ sind nicht durchgehend 'online'.

Da alle drei Consumer über eine eigene Queue verfügen, werden die Messages dort persistiert, bis der Consumer wieder 'online' ist. Obwohl 
_Consumer B_ zum Sendezeitpunkt der _Messages 3_ und 4 nicht 'online' ist, konsumiert er die Nachrichten sobald er wieder mit der Queue 
verbunden ist. _Consumer C_ ist nur zum Sendezeitpunkt der _Messages 3_ und _4_ 'online', konsumiert aber auch die _Messages 1_ und _2_, da
die zugehörige Queue bereits im Vorfeld erstellt wurde und alle bereits versendeten Nachrichten persistiert.


### Simulation Pub/Sub mit Topic

![simulation-topic](../doc/images/simulationTopic.png)

Das topic-basierte Pub/Sub System für die Simulation verfügt über einen Producer und vier Consumer. Die Abbildung zeigt die Konsumierung 
von sechs _Messages_ die in der Simulation versendet werden. Darüber hinaus sind die Unterschiede zwischen durablen (_Consumer D_) und 
non-durablen Consumern (_Consumer A, B, C_) ersichtlich.  

_Consumer A_ ist zum Sendezeitpunkt aller Messages 'online' und konsumiert diese sobald sie gesendet wurden. _Consumer B_, _C_ und _D_ 
sind nur zeitweise 'online'. _Consumer B_ hat eine non-durable Subscription und ist zum Sendezeitpunkt der _Messages 2_, _5_ und _6_ 
'online' und konsumiert diese Nachrichten. _Consumer C_ hat ebenfalls eine non-durable Subscription und ist nur zum Sendezeitpunkt der 
_Message 3_ und _4_ 'online' und komsumiert daher nur die beiden Nachrichten. 

_Consumer D_ hat eine durable Subscription. Damit werden alle Messages die zwischen der ersten Verbindung und der nächsten Verbindung des 
Consumers gesendet werden zwischengespeichert. Obwohl _Consumer D_ zum Sendezeitpunkt der _Message 3_ und _4_ 'offline' war, kann er die 
Nachrichten bei der nächsten Verbindung konsumieren.


### Simulation Pub/Sub mit Topic & shared Subscription

![simulation-topic-shared-sub-schema](../doc/images/simulationTopicSharedSub.png)

Das topic-basierte Pub/Sub System für die Simulation verfügt über einen Producer und zwei Consumer mit einer _shared Subscription_. Bei 
einer _shared Subscriptions_ teilen sich mehrere Instanzen eines _Consumers_ eine _Subscription_. Durch die _shared Subscription_ wird jede
gesendete _Message_ nur von einem _Consumer_ konsumiert. Welcher _Consumer_ dies im Einzelfall ist, lässt sich nicht vorhersagen.
 
In der Simulation (siehe Abbildung) werden sechs _Messages_ versendet und durch jeweils einen der beiden Consumer konsumiert. Da nicht 
vorhersagbar ist welcher Consumer welche Message konsumiert überprüft die Simulation, dass die _Message_ nur durch einem der beiden 
Consumer konsumiert wurde.
