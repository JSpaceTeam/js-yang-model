### YANG as DDL for JUNOS-IQ Services
  

**Introduction**  
We have chosen YANG as the primary modeling and API definition language for config data in JUNOS IQ. Here is diagram from Bruno's [iq-platform-architecture-specification](https://junipernetworks.sharepoint.com/teams/cto/JunosIQ/JunosIQArch/docs/iq-platform-architecture-specification---22-dec-2014---v8.docx):
![](https://raw.githubusercontent.com/JSpaceTeam/js-yang-model/master/docs/images/DataModelDrivenInterface.png?token=AHghsV7xF5AMGXMHleSUeNkymhUhMTMqks5U92gTwA%3D%3D)

This document focuses on using YANG to define data model for the IQ services with IF-MAP semantics. 

**Contrail IF-MAP Data Model Semantics**  
Contrail data model follows IF-MAP graph-based data model semantics. Besides the IF-MAP pub/sub semantics, the data model defined with such semantics can be easily mapped to key-value scale-out database such as Cassandra. The code to access this data model in the database, CRUD operations, can be generated from the modeling language. Implementation of other cross-cutting features, such as RBAC, notifcation, and logging etc, can also be generated automatically from the modeling language.

Here are the basic constructs of IF-MAP based data model:  
***Identity [I]***  
* Node in the IF-MAP data model graph that represents a type of objects  
* Identity can be attached with one or more properties ![](https://raw.githubusercontent.com/JSpaceTeam/js-yang-model/jnpr-tjiang-edit/docs/images/I_P.png?token=AHghsQJ8G4pUt1_J1Em2K7TqzswTyLDbks5U96cSwA%3D%3D)
* Each instance of a certain identity is identified by an UUID generated according to RFC-4122  
* Identities are exposed as REST resources when accessed via REST API  
***Reference [R]***  
There are three types of references  
* *[Ref]*: Strong reference to guarrantee referential integrity. Object referenced object can not be deleted until the reference is removed. ![](https://raw.githubusercontent.com/JSpaceTeam/js-yang-model/jnpr-tjiang-edit/docs/images/ref_link.png?token=AHghsf_1H4TYcMcznXY80yNblwK5XhAeks5U96eywA%3D%3D)
* *[Has]*: Child object can not exist with parent. When parent object is deleted, its linked child objects are deleted. ![](https://raw.githubusercontent.com/JSpaceTeam/js-yang-model/jnpr-tjiang-edit/docs/images/has_link.png?token=AHghsaS822lyJOoHjIrofN67sScz9qcZks5U96o9wA%3D%3D)
* *[Conn]*: Weak reference ![](https://raw.githubusercontent.com/JSpaceTeam/js-yang-model/jnpr-tjiang-edit/docs/images/conn_link.png?token=AHghsWfbLbpS3dEnOaMxwPBoR8Pd-rpBks5U96qawA%3D%3D)  

***Property [P]***  
* One or more properties can be attached to either Identities or References![](https://raw.githubusercontent.com/JSpaceTeam/js-yang-model/jnpr-tjiang-edit/docs/images/P.png?token=AHghsdtj04JsAyROkdqMmhNvzTTczppDks5U96xwwA%3D%3D)
* Property value can be either of primitive types or nested data structure such as JSON  
* Properties are mapped to columns of a Cassandra table.

**Common Data Model vs Service Specific Data Model**  


**YANG as Modeling Language for IF-MAP Data Model**  

**Non-CRUD operation**

**Data Model Change Notification**

**REST API for CRUD operations**

**Map YANG RPC to REST**

**Map YANG notification to REST**


