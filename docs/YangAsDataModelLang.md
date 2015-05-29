##YANG as the modeling language for CSP Micro Services
-----------------  

###1. Introduction
We have chosen YANG as the data modeling and API definition language for micro services in CSP. This means that the designer of a micro service creates a YANG model with the following:
- Data model describing the resources which are managed (and exposed) by the micro service.
- Operational RPCs provided by the micro service to its consumers.
- Notifications that can be sent by the micro service to subscribers.

From this YANG model, tools generate code required to realize the REST API of this micro service. This is illustrated in the following architecture diagram from JUNOS IQ architecture document ([iq-platform-architecture-specification](https://junipernetworks.sharepoint.com/teams/cto/JunosIQ/JunosIQArch/docs/iq-platform-architecture-specification---22-dec-2014---v8.docx)):
![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/DataModelDrivenInterface.png)

This document focuses more on using YANG for defining the data model of a micro service. Aspects related to RPCs and notifications will be dealt with in other documents.

###2. Leveraging Contrail Config Node Infrastructure

We have made a decision to leverage Contrail technologies as much as possible and appropriate. As part of this, the Contrail Config Node can be leveraged as a DBaaS. This is shown in the following architecture diagram.

![](https://github.com/rjoyce/js-yang-model/blob/master/docs/images/contrail-dbaas.png)

The main advantages of leveraging Contrail Config Node as DBaaS are: 

- The data model can be easily mapped to scale-out database such as Cassandra.
- REST API (and its implementation) to access the data model can be generated from data model schema. 
- Other cross-cutting features, such as RBAC, data model change notifcation, and logging, can also be auto-generated from the data model schema.

In this design, the micro service designer creates a YANG model for the service as shown in the top right-hand side of the above diagram. Our tool chain compiles this model and generates the following:

- Provider-side stub code for this service's REST APIs. This is marked as (1) in the diagram. These APIs conform to the [RESTCONF](https://tools.ietf.org/html/draft-ietf-netconf-restconf-04) protocol.
- Provider-side implementation code for those APIs that deal with CRUD of the service's resources. This is marked as (2) in the diagram. This means that the micro service designer does not have to write code that implements the CRUD APIs. This code also implements paging, sorting, and filtering semantics for these APIs.
- An XSD file that describes the data model using Contrail's IF-MAP semantics. This is marked as (3) in the diagram. Contrail IF-MAP semantics are briefly explained in subsequent sections of this document. (See [Section 3](#section3)).

The generated XSD is then compiled by the Contrail code generator tool to generate the following:

- API-server side code to implement the CRUD operations for this data model by persisting the data inside the Cassandra database.
- Java client side code to invoke these CRUD APIs. This Java code is internally used by the provider-side implementation code (marked as (2) in the diagram) to interface with the Contrail Config Node via REST.

The operation RPCs defined in the YANG model is compiled to REST API stubs to be implemented with service specific business logic. The business logic can access the data model via Contrail API Server and listen for data model changes via RabbitMQ. This service specific logic for implementing the RPCs is the main piece of code that the micro service designer needs to write.

PS: The YANG notification section of the service YANG is compiled into implementation that sends notification to REST client via HTML5 server sent events over HTTP. This is not shown in the diagram above.

###3. <a name="section3"></a>Contrail IF-MAP Data Model Semantics
This section intends to give an introduction to Contrail data model semantics. Contrail data model semantics are basically that of a Directed Acyclic Graph. Three basic constructs are `identity` (graph vertex), `reference` (edge between the vertices), and `property` that can be attached to either vertex or edge of graph.

**Identity**  
An identity represents a vertex in the graph data model. It uniquely identify a category of objects in the data model. Each `identity` instance is uniquely identified by an UUID generated according to [RFC-4122](http://www.ietf.org/rfc/rfc4122.txt). In Contrail implementation, each identity is mapped to a table in Cassandra database.  
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/I_P.png)
    
**Reference**  
A reference represents a directional edge between two vertices in the graph data model. Here are three categories of references supported by Contrail:
* ***[Ref]*** - Strong reference that guarantees referential integrity. A referenced object can not be deleted until the reference is removed.
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/ref_link.png)
* ***[Has]*** - This models a parent-child relation. Child object can not exist without parent. When parent object is deleted, its linked children objects are deleted automatically.   
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/has_link.png)
* ***[Conn]*** - Weak reference that does not prevent referenced object from being deleted. It is up to the application code to validate existence of referenced objects.  
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/conn_link.png)  

**Property**  
An `identity` or `reference` in the graph data model can have any number of properties. Each property is a name value pair. The value can be either a primitive type or a nested data structure represented as a JSON object. In Contrail implementation, properties are mapped to columns of the Cassandra table.  
![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/P.png)

Here is an example from Contrail data model:  
![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/vnc.png)

###4. <a name="section4"></a>Representing  Graph Data Model using YANG

In order to leverage the Contrail Config Node infrastructure we need to be able to capture the Contrail graph data model semantics inside the YANG models to be defined for CSP micro services. In order to facilitate this, we have defined a common yang module for CSP micro services. This common module is defined [here](https://github.com/Juniper-CSP/csp-yang-data-model/blob/master/common/src/main/yang/csp-common.yang). This module needs to be imported by all CSP service yang modules. It defines the following:

- a YANG extension named `csp:vertex` that can be used to annotate a data node in a service's data model as forming the vertex of a graph.
- a YANG extension named `csp:has-edge` that can be used to annotate a data node as having a *containment* relation from its parent data node.
- a YANG extension named `csp:ref-edge` that can be used to annotate a data node as having a *strong reference* relation from its parent data node.
- a YANG grouping named `csp:entity` that can be used to *inherit* all the mandatory properties required by a data node that needs to be persisted on the Contrail Config Nodes.

The `csp:vertex` extension is defined as follows:
````
    extension vertex  {
	    description "A vertex maintains pointers to both a set of incoming and outgoing edges.
                    The outgoing edges are those edges for which the vertex is the tail.
                    The incoming edges are those edges for which the vertex is the head.";
    }
````

The `csp:has-edge` extension is defined as follows:
````
    extension has-edge  {
        description "An Edge links two vertices. Along with its key/value properties, an edge has both a directionality and a label.
                    The directionality determines which vertex is the tail vertex (out vertex) and which vertex is the head vertex (in vertex).
                    The edge label determines the type of relationship that exists between the two vertices.";
    }
````

The `csp:ref-edge` extension is defined as follows:
````
    extension ref-edge  {
        description "An Edge links two vertices. Along with its key/value properties, an edge has both a directionality and a label.
                    The directionality determines which vertex is the tail vertex (out vertex) and which vertex is the head vertex (in vertex).
                    The edge label determines the type of relationship that exists between the two vertices.";
    }
````

The `csp:entity` *grouping* is defined as follows:
````
    grouping entity {
        description "Base grouping for all objects that can be identified, access-coontroled, multi-tenantable";
        leaf administrative-domain { type string; }
        leaf type {
            type enumeration {
                enum aik-name;
                enum distinguished-name;
                enum dns-name;
                enum email-address;
                enum hip-hit;
                enum kerberos-principal;
                enum username;
                enum sip-uri;
                enum tel-uri;
                enum other;
            }
        }
        leaf other-definition { type string; }

        // Extensions
        leaf-list fq-name {
            description "FQDN for the IFMAP identity";
            type string;
        }
        leaf uuid {
            description "UUID for the IFMAP identity";
            type yang:uuid;
        }
        leaf href {
            description "HATEOAS HREF for the identity node";
            type inet:uri;
        }
        leaf parent-uuid {
            description "parent node UUID";
            type yang:uuid;
        }
        leaf parent-href {
            description "parent node HATEOAS HREF";
            type inet:uri;
        }
        leaf parent-type {
            description "parent node type";
            type string;
        }
        leaf display-name {
            description "Display name";
            type string;
        }
    }
````

Now let's see how these constructs can be used in a service specific data model. As an example, let's consider a Device Management service which manages a list of device resources. Each device resource contains a list of configuration-versions that are archived for the device. Also the service maintains a list of scripts that can be associated with devices. In this example, 

- There are 3 vertices - device, configuration-version, script.
- There is a has-edge containment relationship between a device and its configuration-versions.
- There is a ref-edge relationship between a device and a set of scripts.

This is shown in the following YANG snippet:

````
	list device {
		uses csp:entity;
		csp:vertex;
		
		key uuid;
		
		leaf host-name {
			type string;
		}
		
		leaf ip-address {
			type string;
		}
		
		list config-version {
			csp:has-edge;
			key uuid;
			
			leaf uuid {
				type leafref {
					path "/config-version/uuid"
				}
			}
			
			leaf archived-at {
				type yang:datetime;
			}
		}
		
		list script {
			csp:ref-edge;
			key uuid;
			
			leaf uuid {
				type leafref {
					path "/script/uuid"
				}
			}
			
			leaf deployed-at {
				type yang:datetime;
			}
		}
	}
	
	list config-version {
		uses csp:entity;
		csp:vertex;
		
		key uuid;
		
		leaf version-num {
			type uint16;
		}
		
		leaf config-text {
			type string;
		}
	}
	
	list script {
		uses csp:entity;
		csp:vertex;
		
		key uuid;
		
		leaf content {
			type string;
		}
	}
````

Please note the following points about the above YANG model:

- The `csp:vertex` statement is used to mark the device, config-version, and script data nodes as vertices in the graph data model.
- The `csp:entity` grouping is used inside these nodes to inherit the mandatory properties required by all vertices.
- Child nodes of a node that uses `csp:entity` become properties of that vertex (columns in the Cassandra table for this vertex). For example, `host-name` and `ip-address` are properties of the `device` vertex.
- The `csp:has-edge` statement is used to mark the list `/device/config-version` as modeling a containment relationship from the device resource to configuration-version resources. Each node in this list has one property `uuid` which is a leafref pointing to the actual configuration-version at the XPath `/configuration-version/uuid`.
- The `csp:ref-edge` statement is used to mark the list `/device/script` as modeling a reference relationship from the device resource to script resources. Each node in this list has one property `uuid` which is a leafref pointing to the actual script at the XPath `/script/uuid`.
- Child nodes of a node marked as `csp:has-edge` or `csp:ref-edge` becomes properties of that edge. For example, `archived-at` becomes a property of the containment edge from the device to configuration-version.

###5. <a name="section5"></a>Contrail XSD Internals ###

This section explains the conventions used by Contrail when using XSD as Modeling Language for IF-MAP Data Model. Our toolchain generates XSD files that follow these conventions and hence these XSD files can be used by Contrail compiler as shown in the architecture diagram at the top.

**Identity**

Any top level schema node with type `ifmap:IdentityType` is an IF-MAP identity. 
```
	<xsd:element name="virtual-network" type="ifmap:IdentityType"/>
```
Here is the definition of `ifmap:Identity` from  [ifmap-base-2.0.xsd](https://github.com/ITI/ifmap-python-client/blob/master/schema/ifmap-base-2.0.xsd):
```
	<!-- IdentityType Identifier represents an end-user -->
	<xsd:complexType name="IdentityType">
		<xsd:attribute name="administrative-domain" type="xsd:string" />
		<xsd:attribute name="name" type="xsd:string" use="required" />
		<xsd:attribute name="type" use="required">
			<xsd:simpleType>
				<xsd:restriction base="xsd:string">
					<xsd:enumeration value="aik-name" />
					<xsd:enumeration value="distinguished-name" />
					<xsd:enumeration value="dns-name" />
					<xsd:enumeration value="email-address" />
					<xsd:enumeration value="hip-hit" />
					<xsd:enumeration value="kerberos-principal" />
					<xsd:enumeration value="username" />
					<xsd:enumeration value="sip-uri" />
					<xsd:enumeration value="tel-uri" />
					<xsd:enumeration value="other" />
				</xsd:restriction>
			</xsd:simpleType>
		</xsd:attribute>
		<xsd:attribute name="other-type-definition" type="xsd:string" />
	</xsd:complexType>
```

**Property**

XSD comment `<!--#IFMAP-SEMANTICS-IDL Property()-->` is used to mark a XSD element as a property of an identity. Here is an example of defining properties for the `virtual-network` identity. 
```
   <xsd:element name="virtual-network-properties" type="VirtualNetworkType" />
   <!--#IFMAP-SEMANTICS-IDL Property('virtual-network-properties', 'virtual-network') -->

   <xsd:complexType name="VirtualNetworkType">
      <xsd:all>
         <xsd:element name="allow-transit" type="xsd:boolean" />
         <!-- A unique id for the network, auto generated -->
         <xsd:element name="network-id" type="xsd:integer" />
         <!-- VNI for the network, configured by user -->
         <xsd:element name="vxlan-network-identifier" type="VxlanNetworkIdentifierType" />
         <!-- Forwarding mode for virtual-network  -->
         <xsd:element name="forwarding-mode" type="ForwardingModeType" />
      </xsd:all>
   </xsd:complexType
   <xsd:simpleType name="VxlanNetworkIdentifierType">
      <xsd:restriction base="xsd:integer">
         <xsd:minInclusive value="0" />
         <xsd:maxInclusive value="1048575" />
      </xsd:restriction>
   </xsd:simpleType>
   <xsd:simpleType name="ForwardingModeType">
      <xsd:restriction base="xsd:string">
         <xsd:enumeration value="l2_l3" />
         <xsd:enumeration value="l2" />
      </xsd:restriction>
   </xsd:simpleType>
```

**Link**

XSD comment `<!--#IFMAP-SEMANTICS-IDL Link()-->` is used to mark a XSD element as a link from one identity to another. Here is an example of defining a link from virtual-network to network policy. Note that the link XSD element can be a nested structure with child nodes as the link property. In this the following example, `sequence` and `timer` are the properties for the `virtual-network-network-policy` link.
```
   <xsd:element name="virtual-network-network-policy" type="VirtualNetworkPolicyType" />
   <!--#IFMAP-SEMANTICS-IDL Link('virtual-network-network-policy', 'virtual-network', 'network-policy', ['ref']) -->
   
   <xsd:complexType name="VirtualNetworkPolicyType">
      <xsd:all>
         <xsd:element name="sequence" type="SequenceType" />
         <xsd:element name="timer" type="TimerType" />
      </xsd:all>
   </xsd:complexType>
```

###7. <a name="section7"></a>RESTCONF API to access the data model

TODO: Link to Juntao's document.

###8. <a name="section8"></a>RESTCONF API for operations
TODO

###9. <a name="section9"></a>YANG notifications
TODO


