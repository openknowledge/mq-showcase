# MQ Pub/Sub

Zurück zur [README](README.md)

**Inhaltsverzeichnis**

* [1 Pub/Sub Grundlagen](#1Pub/SubGrundlagen)
    * [1.1 Subscribers & Subscriptions](#1.1Subscribers&Subscriptions)
    * [1.2 Topics](#1.2Topics)
    * [1.3 Alias Queue](#1.3AliasQueue)
    * [1.4 Dead Letter Queue / Backout Queue](#1.4DeadLetterQueue/BackoutQueue)
* [2 Pub/Sub in der Praxis](#2Pub/SubinderPraxis)
    * [2.1 Pub/Sub mit Queues](#2.1Pub/SubmitQueues)
    * [2.2 Pub/Sub mit Topics](#2.2Pub/SubmitTopics)
    * [2.3 MQ Konfiguration](#2.3MQKonfiguration)
    * [2.4 Unterschiede](#2.4Unterschiede)


## 1 Pub/Sub Grundlagen <a name="1Pub/SubGrundlagen"></a>

Message Broker wie IBM MQ erlauben die Entkopplung des Produzenten einer Information von den Konsumenten der Information. Der Vorteil einer 
entkoppelten Kommunikation besteht darin, dass die sendende Anwendung und die empfangene Anwendung nichts voneinander zu wissen brauchen, 
um Informationen zu senden und zu empfangen. Wie nahezu alle Message Broker ermöglicht IBM MQ unterschiedliche Arten von Kommunikation: 
Punkt-zu-Punkt sowie Publish/Subscribe - oftmals als _Pub/Sub_ abgekürzt.

Bevor eine Nachricht über eine Punkt-zu-Punkt Verbindung zwischen zwei Anwendungen verschickt werden kann, muss der Sender etwas über den 
Empfänger wissen - z.B. den Namen der Queue die der Empfänger konsumiert sowie das Nachrichtenformat, dass der Empfänger versteht. Eine 
Punkt-zu-Punkt Verbindung wird daher durch den Empfänger bestimmt. Darüber hinaus kann eine Nachricht nur von einer einzigen konsumierenden
Anwendung verarbeitet werden.

Pub/Sub beschreibt einen Mechanismus, beim dem Empfänger Informationen in Form von Nachrichten von Sendern empfangen können. Sender - im 
Umfeld asynchroner Kommunikation allgemein als Produzenten bezeichnet - von Informationen wird im Kontext von Pub/Sub als _Publisher_ 
bezeichnet. Publisher stellen Informationen in Form von Nachrichten (_Publication_) zu einem Thema (_Topic_) zur Verfügung. Empfänger - im 
Umfeld asynchroner Kommunikation allgemein als Konsumenten bezeichnet - der Informationen werden im Kontext von Pub/Sub als _Subscriber_ 
bezeichnet. _Subscriber_ die sich für Informationen zu einem bestimmten Thema (_Topic_) interessieren, können diese abonnieren, in dem sie 
eine _Subscription_ für das jeweilige _Topic_ erstellen. Ein _Subscriber_ kann durchaus mehrere _Subscriptions_ abschließen und somit 
Informationen von vielen verschiedenen _Publishern_ zu unterschiedlichen Themen erhalten. Im Unterschied zur Punkt-zu-Punkt Verbindung hebt
Pub/Sub - durch den Einsatz von Topics - die Notwendigkeit für den Sender auf, etwas über den Empfänger wissen zu müssen. Gleichzeitig muss
der Empfänger nichts über die Quelle der Information zu wissen. Ein weiterer signifikanter Unterschied ist, dass eine Nachricht, die auf 
einem Topic veröffentlicht wird, von allen interessierten Empfängern verarbeitet werden kann.

_weitere Information zu Pub/Sub_
*   [Publish/Subscribe Pattern](https://en.wikipedia.org/wiki/Publish–subscribe_pattern)
*   [IBM MQ 9.0.x - Publish/Subscribe Messaging](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q004870_.htm)


### 1.1 Subscribers & Subscriptions <a name="1.1Subscribers&Subscriptions"></a>

Ein typisches Pub/Sub System besteht i.d.R. aus 1-n Publishern und 1-n Subscribern sowie einer Vielzahl unterschiedlicher Topics. Bei IBM 
MQ werden sämtlichen Interaktionen zwischen Publishern und Subscribern durch einen bzw. mehrere Queue-Manager gesteuert. Der Queue-Manager
empfängt Nachrichten von Publishern und Subscriptions von Subscribern zu einer Reihe von Topics. Die Aufgabe des Queue-Manager besteht 
darin, die veröffentlichten Nachrichten an die Subscriber weiterzuleiten, die eine Subscription für das jeweiligen Topic erstellt haben. 
Eine Subscription kann entweder von einer Anwendung oder manuell (via MQSC Command) an den lokalen Queue-Manager gestellt werden. In der 
Subscription ist definiert für welches Topic oder welchen Topic-Baum (die Verwendung von Wildcard ist möglich) sich der Subscriber 
interessiert. Des weiteren können folgende Parameter festgelegt werden:

*   ein optionaler Selection-String um nach Publikation mit bestimmten Eigenschaften zu filtern
*   eine optionale Subscriber Queue, in der die Publikationen abgelegt werden
*   sowie eine ebenfalls optionale Correlation-ID. 

Erhält der lokale Queue-Manager eine Publikation, prüft er ob eine oder mehrere Subscriptions für das zugehörige Topic existieren und die 
Publikation den definierten Filter-Kriterien (Selection-String) entspricht. Falls ja, leitet er die Publikation in die jeweiligen Subscriber
Queues.

_weitere Information zu Subscribern & Subscriptions_
*   [IBM MQ 9.0.x - Publish/Subscribe Components](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q004890_.htm)
*   [IBM MQ 9.0.x - Subscribers and subscriptions](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q004950_.htm)

#### 1.1.1 Durable & Non-Durable Subscriptions

Eine Subscription kann als durable (dauerhaft) oder non-durable (nicht dauerhaft) Subscription konfiguriert werden. Die Konfiguration der 
Subscription bestimmt, was mit der Subscription geschieht, wenn die Verbindung der Subscriber Anwendung zum Queue-Manager getrennt wird. 

Wie der Name vermuten lässt, bleibt eine durable Subscription bestehen, wenn die Verbindung der Subscriber Anwendung zum Queue-Manager 
getrennt wird. Die Subscription bleibt auch nach dem Trennen der Verbindung bestehen und kann durch die Subscriber Anwendung 
wiederhergestellt werden. Um eine durable Subscription herzustellen, muss die Subscriber Anwendung beim Erstellen der Subscription einen 
eindeutigen Namen (`SubName`) festlegen und diesen bei jedem Verbindungsaufbau übergeben. Wird eine durable Subscription nicht länger 
benötigt, kann diese durch die Subscriber Anwendung oder manuell über den Queue Manager gelöscht werden.

Solange eine durable Subscription besteht, werden Publikationen in die Subscriber Queue weitergeleitet auch wenn die Verbindung der 
Subscriber Anwendung zum Queue-Manager getrennt ist. Bleibt die Verbindung über einen längeren Zeitraum getrennt, können eine Vielzahl von 
nicht verarbeiteten Publikationen in der Subscriber Queue auflaufen. Dies kann im schlechtesten Fall zu einem Speicherüberlauf und dem 
Absturz des Queue-Managers führen. Um dieses Problem zu vermeiden, empfiehlt es sich non-durable Subscriptions wann immer sinnvoll möglich 
zu verwenden.

Eine non-durable Subscription besteht nur solange wie Verbindung der Subscriber Anwendung zum Queue-Manager besteht. Wird die Verbindung 
zum Queue-Manager getrennt, wird die Subscription automatisch entfernt und keine weiteren Publikationen an die Subscriber Queue 
weitergeleitet.

_weitere Information zu Durable & Non-Durable Subscriptions_
*   [IBM MQ 9.0.x - Managed queues and publish/subscribe](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q004960_.htm)
*   [IBM MQ 9.0.x - Subscription durability](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q004970_.htm)

#### 1.1.2 Cloned & Shared Subscription

![shared sub](doc/images/pubSubTopicShared.png)
    
IBM MQ unterstützt zwei Methoden - durch die Verwendung geklonter (cloned) oder gemeinsam genutzter (shared) Subscriptions - um mehreren 
Subscribern Zugriff auf dieselbe Subscription zu gewähren. Was auf den ersten Blick, wie ein Widerspruch zum Pub/Sub Mechanismus aussieht, 
deckt einen praktischen Bedarf insbesondere von Anwendungen die mit mehreren Instanzen betrieben werden. Werden für eine Anwendung mehrere 
Instanzen betrieben, würden in einem Pub/Sub-System die Nachricht n mal konsumiert und verarbeitet. Mittels geklonter oder shared 
Subscription wird hingegen jede Nachricht von jeder Anwendung nur einmal verarbeitet.
    
Geklonte Subscriptions sind eine IBM MQ Erweiterung, die mehreren Verbrauchern in verschiedenen JVMs gleichzeitigen Zugriff auf die 
Subscription ermöglicht. Sie können nur bei durable Subscriptions aktiviert werden. Eine durable Subscription kann als geklont betrachtet 
werden, wenn ein oder mehrere Subscriber unter Angabe demselben Subscription Name erstellt wurden. Wird eine Nachricht auf dem Topic der 
Subscription veröffentlicht, wird eine Kopie dieser Nachricht an die Subscription gesendet. Die Nachricht steht jedem der Subscriber zur 
Verfügung, wird aber nur von einem empfangen.

Mit JMS 2.0 wurden shared Subscriptions eingeführt, die es mehreren Subscribern einer Topic Subscription ermöglichen, eine Nachrichten 
gemeinsam zu nutzen. Jede Nachricht aus der Subscription wird anstatt allen nur einem Subscriber zugestellt. Shared Subscriptions können 
sowohl für durable als auch non-durable Subscriptions erstellt werden.

_weitere Information zu Shared Subscriptions_
*  [IBM MQ 9.0.x - Cloned & Shared Subscriptions](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.dev.doc/q119140_.htm)


### 1.2 Topics <a name="1.2Topics"></a>

Ein Topic beschreibt das Thema der Informationen, zu dem Nachrichten über das jeweilige Topic versendet werden. Will ein Publisher eine 
Publikation versenden, gibt er das Topic in Form eines Topicstrings an. Um diese und weitere Publikationen zu erhalten, muss ein Subscriber
beim Erstellen einer Subscription einen `passenden` Topicstring angeben. Als Topicstring kann sowohl der exakte Namen des Topics als auch 
ein Muster mit Platzhaltern verwendet werden, das zum Topic der Publikation passt. Letztere sind im Zusammenhang mit Topic-Trees von 
Bedeutung.

In größeren Organisation werden Themen hierarchisch in Topic-Trees organisiert. Während ein Publisher eine Publikation immer an ein 
ausgewähltes Topic z.B. `/huk/kfz/pkw` sendet, kann ein Subscriber eine Subscription auf einen Topic-Tree, i.d.R. einen Teilbaum bestehend 
aus mehreren hierarchisch angeordneten Topics z.B. `/huk/kfz/+` erstellen. In diesem Fall leitet der Queue-Manager nicht nur Publikationen 
des Topic `/huk/kfz/pkw` sondern auch für die Topics `/huk/kfz/motorad` und `/huk/kfz/wohnwagen` an den Subscriber weiter.

_weitere Information zu Topics_
*   [IBM MQ 9.0.x - Topics](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q004990_.htm)
*   [IBM MQ 9.0.x - Topic strings](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q005000_.htm)
*   [IBM MQ 9.0.x - Topic trees](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q005050_.htm)


### 1.3 Alias Queue <a name="1.3AliasQueue"></a>

IBM MQ bietet die Möglichkeit Alias-Queues definieren, um indirekt auf eine andere Queue oder ein Topic zu verweisen. Eine Alias-Queue ist 
keine echte Queue, sondern ein Verweis, der zur Laufzeit in eine lokale bzw. entfernte Queue oder ein Topic aufgelöst wird. Alias-Queue 
sind nützlich um ...

*   Anwendungen unterschiedliche Zugriffsberechtigungen auf eine Queue oder ein Topic zu geben
*   Anwendungen zu ermöglichen auf unterschiedliche Weise (z.B. verschiedene Prioritäten) mit derselben Queue oder demselben Topic arbeiten 
können
*    die Wartung, Migration oder die Lastverteilung für eine Queue oder ein Topic zu vereinfachen

Ein typischer Anwendungsfall für Alias-Queues ist die Namensänderungen einer Queue oder eines Topics. Mit Hilfe der Alias-Queue können 
Anwendungen ohne Anpassung mit der umbenannten Queue oder dem Topic kommunizieren.

_weitere Information zu Fehlerhandling_
*   [IBM MQ 9.0.x - Alias Queues](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q003120_.htm)
*   [IBM MQ 9.0.x - Working with alias queues](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.adm.doc/q020820_.htm)


### 1.4 Dead Letter Queue / Backout Queue <a name="1.4DeadLetterQueue/BackoutQueue"></a>

Wenn eine Nachricht nicht zugestellt werden kann, kann im Unterschied zur synchronen Kommunikation der Publisher nicht aufgefordert werden,
die Nachricht zu einem späteren Zeitpunkt erneut zu Versenden. Abhilfe bieten Queues für unzustellbare Nachrichten _Dead Letter Queue_,
kurz _DLQ_, bzw. _Backout Queue_ genannt. 

Im Falle eines Fehlers können Nachrichten durch den Queue-Manager oder eine Anwendung in die Dead Letter Queue bzw. Backout Queue 
verschoben werden.

![dead letter queues](doc/images/deadLetterQueue.png)

Dead Letter Queues und Backout Queues werden für unterschiedliche Zwecke genutzt. Die Dead Letter Queue wird i.d.R. globel für den Broker
konfiguriert und für Nachrichten verwendet, die aufgrund eines technischen Problems (Ziel-Queue ist voll oder nicht mehr vorhanden) oder 
einer veränderten Konfiguration innerhalb von MQ nicht an ihr vorgesehenes Ziel weitergeleitet werden können. Tritt ein derartiges Problem 
auf werden die betroffenen Nachricht in die Dead Letter Queue verschoben. Nach der Behebung des Problems kann die Nachricht erneut in die 
vorgesehene Queue bzw. das vorgesehen Topic verschoben werden.   

Die Backout Queue wird i.d.R. individuell je Queue konfiguriert und verwendet wenn ein Subscriber eine Nachricht wiederholt nicht 
verarbeiten kann. Ursachen sind entweder eine _Poison Message_ (Nachricht mit einem falschen Format) oder ein fehlerhaft arbeitender 
Subscriber. Damit der Subscriber oder die Queue bzw. das Topic nicht dauerhaft blockiert wird, wird die Nachricht nach einer definierten 
Zahl von Zustellversuchen ausgesteuert und in die Backout Queue verschoben. Nach der Behebung des Problems kann die Nachricht erneut in die 
vorgesehene Queue bzw. das vorgesehen Topic verschoben werden oder direkt aus der Backout Queue konsumiert und verarbeitet werden.     

_weitere Information zu Fehlerhandling_
*   [IBM MQ 9.0.x - Safety of messages](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.con.doc/q015720_.htm)
*   [IBM MQ 9.0.x - Dead-letter queues](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.pro.doc/q002680_.htm)
*   [IBM MQ 9.0.x - Working with dead-letter queues](https://www.ibm.com/support/knowledgecenter/SSFKSJ_9.0.0/com.ibm.mq.adm.doc/q020730_.htm)



## 2 Pub/Sub in der Praxis <a name="2Pub/SubinderPraxis"></a>

Mit IBM MQ kann Pub/Sub auf zwei Arten realisiert werden - mit Topics oder mit Queues.

### 2.1 Pub/Sub mit Queues <a name="2.1Pub/SubmitQueues"></a>

![topic schema](doc/images/schemaQueue.png)

Die Abbildung zeigt ein Pub/Sub System mit einem Publisher (Producer) und drei Subscribern (Consumer A/B/C) die über eine Alias Queue, ein 
Topic sowie drei Queues (eine je Consumer) kommunizieren. Der Producer _(vgl. Maven Modul queue-producer)_ und die Consumer _(vgl. Maven 
Modul queue-consumer)_ sind JEE Anwendungen die Nachrichten über Queues austauschen. Die Publisher Anwendung (Producer) sendet Nachrichten 
an die Alias Queue. Diese werden an das Topic `DEV.BASE.TOPIC` weitergeleitet. Für jede Queue  `DEV.QUEUE.1`, `DEV.QUEUE.2` und 
`DEV.QUEUE.3` wird eine Subscription auf dem Topic erstellt, welche dafür sorgt, dass die Nachricht vom Topoi an die jeweilige Queue 
weiterleitet_._ An jeder Queue liest eine Consumer Anwendung die eingehenden Nachrichten. 

Vor dem Versenden der ersten Nachricht müssen die Alias Queue, das Topic sowie eine Queue für jeden Subscriber im Queue-Manager 
konfiguriert werden. Für jeden weiteren Subscriber muss eine zusätzliche Queue konfiguriert werden.

_Die Funktionweise lässt sich anhand des folgenden Beispiels nachvollziehen:_ 

![simulation-queue](doc/images/simulationQueue.png)

Das queue-basierte Pub/Sub System für das Beispiel verfügt über einen Producer und drei Consumer. Die Abbildung zeigt die Konsumierung von 
sechs _Messages_ die in der Simulation versendet werden. 

_Consumer A_ ist zum Sendezeitpunkt aller Messages 'online' und konsumiert die Nachrichten sobald diese gesendet wurden. _Consumer B_ und 
_Consumer C_ sind nicht durchgehend 'online'.

Da alle drei Consumer über eine eigene Queue verfügen, werden die Messages dort persistiert, bis der Consumer wieder 'online' ist. Obwohl 
_Consumer B_ zum Sendezeitpunkt der _Messages 3_ und 4 nicht 'online' ist, konsumiert er die Nachrichten sobald er wieder mit der Queue 
verbunden ist. _Consumer C_ ist nur zum Sendezeitpunkt der _Messages 3_ und _4_ 'online', konsumiert aber auch die _Messages 1_ und _2_, da
die zugehörige Queue bereits im Vorfeld erstellt wurde und alle bereits versendeten Nachrichten persistiert.

### 2.2 Pub/Sub mit Topics <a name="2.2Pub/SubmitTopics"></a>

![topic schema](doc/images/schemaTopic.png)

Die Abbildung zeigt ein Pub/Sub System mit einem Publisher (Producer) und drei Subscribern (Consumer A/B/C) die über Topic kommunizieren. 
Der Producer _(vgl. Maven Modul topic-producer)_ und die Consumer _(vgl. Maven Module topic-consumer)_ sind JEE Anwendungen, die 
Nachrichten über Topics austauschen. Die Publisher Anwendung (Producer) sendet Nachrichten an das Topic `DEV.BASE.TOPIC`. Diese werden an 
alle Subscriber Anwendungen (Consumer A/B/C) weitergeleitet, die eine Subscription auf dem Topic haben. 

Vor dem Versenden der ersten Nachricht muss das Topic im Queue-Manager konfiguriert werden. Weitere Subscriber Anwendungen können 
selbstständig und ohne zusätzlichen Konfigurationsaufwand eine Subscription auf den Topic erstellen.

_Die Funktionweise lässt sich anhand des folgenden Beispiels nachvollziehen:_ 

![simulation-topic](doc/images/simulationTopic.png)

Das topic-basierte Pub/Sub System für das Beispiel verfügt über einen Producer und vier Consumer. Die Abbildung zeigt die Konsumierung von 
sechs _Messages_ die in der Simulation versendet werden. Darüber hinaus sind die Unterschiede zwischen durablen (_Consumer D_) und 
non-durablen Consumern (_Consumer A, B, C_) ersichtlich.  

_Consumer A_ ist zum Sendezeitpunkt aller Messages 'online' und konsumiert diese sobald sie gesendet wurden. _Consumer B_, _C_ und _D_ sind
nur zeitweise 'online'. _Consumer B_ hat eine non-durable Subscription und ist zum Sendezeitpunkt der _Messages 2_, _5_ und _6_ 'online' 
und konsumiert diese Nachrichten. _Consumer C_ hat ebenfalls eine non-durable Subscription und ist nur zum Sendezeitpunkt der _Message 3_ 
und _4_ 'online' und komsumiert daher nur die beiden Nachrichten. 

_Consumer D_ hat eine durable Subscription. Damit werden alle Messages die zwischen der ersten Verbindung und der nächsten Verbindung des 
Consumers gesendet werden zwischengespeichert. Obwohl _Consumer D_ zum Sendezeitpunkt der _Message 3_ und _4_ 'offline' war, kann er die 
Nachrichten bei der nächsten Verbindung konsumieren.

#### 2.2.1 Pub/Sub mit Topics & shared Subscription

Eine Mischung zwischen Pub/Sub mit Queues und Pub/Sub mit Topics besteht bei der Verwendung von shared Subscriptions.

Der Producer _(vgl. Maven Modul topic-producer)_ und die Consumer _(vgl. Maven Module topic-consumer)_ sind JEE Anwendungen, die 
Nachrichten über Topics austauschen. Die Publisher Anwendung (Producer) sendet Nachrichten an das Topic 
`DEV.BASE.TOPIC`. Diese werden im Unterschied zum vorherigen Beispiel nicht an alle sondern nur einen der Subscriber weitergeleitet, die 
sich eine Subscription auf dem Topic teilen. 

_Die Funktionweise lässt sich anhand des folgenden Beispiels nachvollziehen:_ 

![simulation-topic-shared-sub-schema](doc/images/simulationTopicSharedSub.png)

Das topic-basierte Pub/Sub System für das Beispiel verfügt über einen Producer und zwei Consumer mit einer _shared Subscription_. Bei einer
_shared Subscriptions_ teilen sich mehrere Instanzen eines _Consumers_ eine _Subscription_. Durch die _shared Subscription_ wird jede 
gesendete _Message_ nur von einem _Consumer_ konsumiert. Welcher _Consumer_ dies im Einzelfall ist, lässt sich nicht vorhersagen.
 
In der Simulation (siehe Abbildung) werden sechs _Messages_ versendet und durch jeweils einen der beiden Consumer konsumiert. Da nicht 
vorhersagbar ist welcher Consumer welche Message konsumiert überprüft die Simulation, dass die _Message_ nur durch einem der beiden 
Consumer konsumiert wurde.


### 2.3 MQ Konfiguration <a name="2.3MQKonfiguration"></a>

#### 2.3.1 Queue erstellen

Ein Queue Objekt kann über das das MQ Dashboard (Admin-UI) oder über ein MQSC Kommando (via Command Line Interface). Alias Queues und 
Subscriptions hingegen können nicht über das MQ Dashboard sondern nur über MQSC Kommandos erstellt werden. Nachfolgend werden die MQSC 
Befehle dargestellt.

**Alias Queue erstellen (init.mqsc)**
```MQSC
DEFINE QALIAS(DEV.QUEUE.ALIAS) TARGTYPE(TOPIC) TARGET(DEV.BASE.TOPIC)
```

Alias Queues können sowohl Queues als auch Topics als Ziel haben. Mit `QALIAS(DEV.QUEUE.ALIAS)` definiert man den Namen der Queue. 
Zusätzlich muss man mit `TARGTYPE(TOPIC) TARGET(DEV.BASE.TOPIC)` den Typ und das Ziel der Alias Queue definieren.


**Subscriptions erstellen (init.mqsc)**
```MQSC
DEFINE SUB(SUB.1) DEST(DEV.QUEUE.1) TOPICOBJ(DEV.BASE.TOPIC)
DEFINE SUB(SUB.2) DEST(DEV.QUEUE.2) TOPICOBJ(DEV.BASE.TOPIC)
DEFINE SUB(SUB.3) DEST(DEV.QUEUE.3) TOPICOBJ(DEV.BASE.TOPIC)
```
Subscriptions benötigen eine Destination Queue und ein Topic als Quelle. Mit `SUB(SUB.1)` definiert man den Namen der Subscription. 
Zusätzlich muss man mit `DEST(DEV.QUEUE.1) TOPICOBJ(DEV.BASE.TOPIC)` Ziel und Quelle für die Subscription definieren.

##### 2.3.1.1 Backout Queue für Queues erstellen

_Backout Queues_ sind normale _local queues_, die genau wie diese erzeugt werden können. Damit der _Queue Manager_ z.B. eine nicht 
zustellbare Nachricht auch in die zugehörige Backout Queue verschiebt, muss diese vorab für alle Queues konfiguriert werden. 

```mqsc
DEFINE QLOCAL(DEV.QUEUE.BO)
...

DEFINE QLOCAL(DEV.QUEUE.MESSAGES.1) BOTHRESH(2) BOQNAME(DEV.QUEUE.BO)
DEFINE QLOCAL(DEV.QUEUE.MESSAGES.2) BOTHRESH(2) BOQNAME(DEV.QUEUE.BO)
DEFINE QLOCAL(DEV.QUEUE.MESSAGES.3) BOTHRESH(2) BOQNAME(DEV.QUEUE.BO)
```

Darüber hinaus bietet MQ die Möglichkeit eine Default-Konfiguration zu definieren. Dazu wird die _SYSTEM.DEFAULT.LOCAL.QUEUE_ erweitert:

```mqsc
ALTER QLOCAL(SYSTEM.DEFAULT.LOCAL.QUEUE) BOTHRESH(2) BOQNAME(DEV.QUEUE.BO)
``` 

Basierend auf der Default-Konfiguration können dann neue Queues erstellt werden, welche dieselben Eigenschaften haben wie die 
_SYSTEM.DEFAULT.LOCAL.QUEUE_.

```mqsc
DEFINE QLOCAL(DEV.QUEUE.MESSAGES.1) LIKE(SYSTEM.DEFAULT.LOCAL.QUEUE)
DEFINE QLOCAL(DEV.QUEUE.MESSAGES.2) LIKE(SYSTEM.DEFAULT.LOCAL.QUEUE)
DEFINE QLOCAL(DEV.QUEUE.MESSAGES.3) LIKE(SYSTEM.DEFAULT.LOCAL.QUEUE)
```
 
#### 2.3.2 Topic erstellen

Ein Topic Objekt kann über das das MQ Dashboard (Admin-UI) oder über ein MQSC Kommando (via Command Line Interface) erstellt werden. 
Nachfolgend wird der MQSC Befehl dargestellt.

**Topic erstellen (init.mqsc)**
```MQSC
DEFINE TOPIC(DEV.BASE.TOPIC) TOPICSTR('dev/')
```

Mit `TOPIC(DEV.BASE.TOPIC)` definiert man den Namen des Topics. `TOPICSTR('dev/')` erstellt einen Topicstring, anhand dessen Subscriptions 
zwischen Topics unterscheiden können.

##### 2.3.2.1 Backout Queue für Topics erstellen

Bei Topics unterscheidet sich der Umgang mit nicht zustellbaren Nachrichten je nach Art der Subscription. - durable bzw. non-durable - bei 
der Konfiguration berücksichtigt werden muss.

_Backout Queues_ können nicht direkt an den Topic-Definitionen erstellt werden. Anstelle einer _local Queue_ wird eine _managed Queue_ 
erstellt, die für durable (`SYSTEM.NDURABLE.MODEL.QUEUE`) und non-durable Subscriptions (`SYSTEM.DURABLE.MODEL.QUEUE`) konfiguriert werden 
kann.

```mqsc
DEFINE QLOCAL('BASE.DEAD.LETTER')
ALTER QMODEL(SYSTEM.DURABLE.MODEL.QUEUE) BOTHRESH(2) BOQNAME(BASE.DEAD.LETTER)
ALTER QMODEL(SYSTEM.NDURABLE.MODEL.QUEUE) BOTHRESH(2) BOQNAME(BASE.DEAD.LETTER)
```

_weitere Informationen zu Backout Queue bei Topics_
* [Handling poison messages in IBM MQ classes for JMS](https://www.ibm.com/support/knowledgecenter/SSFKSJ_8.0.0/com.ibm.mq.dev.doc/q032280_.htm)


## 2.4 Unterschiede <a name="2.4Unterschiede"></a>

Auf den ersten Blick sind beide Lösungen sehr ähnlich. Dies zeigt sich vor allem mit Blick die [JMS Integration](README.md), wo es nur 
marginale Unterschiede gibt. Anders sieht es bei der Administration aus. Soll Pub/Sub über ein Topic realisiert werden, ist nur ein sehr 
geringer administrativer Aufwand erforderlich, da lediglich das Topic erstellt werden muss. Die Erzeugung einer durable Subscription erfolgt 
client-seitig in der konsumierenden Anwendung und erfordert somit keine zusätzlichen administrativen Aufwände. Subscriber können jederzeit
ohne zusätzlichen Aufwand hinzugefügt oder entfernt werden. Administrative Eingriffe sind lediglich erforderlich, wenn die Subscription 
eines durable Subscribers gelöscht werden soll.

Soll Pub/Sub mit Hilfe von Queues abgebildet werden, erhöht sich der Aufwand, da für jeden Subscriber eine Queue und eine Subscription 
administrativ angelegt werden muss. Der Aufwand für die Einrichtung steigt bei einer großen Anzahl von Subscribern linear an. Darüber 
hinaus ist für jeden Subscriber der zu einem späteren Zeitpunkt hinzugefügt wird, ein erneuter administrativer Eingriff erforderlich.