##YANG as DDL for JUNOS-IQ Services
-----------------  

###1. Introduction
We have chosen YANG as the primary modeling and API definition language for config data in JUNOS IQ. This document focuses on using YANG to define data model for the IQ services with IF-MAP semantics. Here is architecture diagram from JUNOS IQ architecture document ([iq-platform-architecture-specification](https://junipernetworks.sharepoint.com/teams/cto/JunosIQ/JunosIQArch/docs/iq-platform-architecture-specification---22-dec-2014---v8.docx)):
![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/DataModelDrivenInterface.png)

###2. Leveraging Contrail Config Node Infrastructure
As stated in the JUNOS IQ architecture document, we are leveraging Contrail as the starting point for JUNOS IQ service-oriented-architecture (SOA). Here is one design of IQ service that leverages Contrail config node infrastructure, including Contrail IF-MAP data model compiler.

![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/iq_contrail.png)

In this design, YANG is chosen to define the data model and operations of an IQ service. The REST APIs to access the data model and operations are generated from the YANG schema according to [RESTCONF](https://tools.ietf.org/html/draft-ietf-netconf-restconf-04) protocol. There are 3 sections in the service YANG definition: a section to defined the data model, a section to define non-CRUD operations supported by the service, and a section to define service notifications.

In order to leverage Contrail infrastructure, the data model defined in the service YANG needs to follow the IF-MAP data model semantics (See [Section 3](#section3)). The data model section of the service YANG schema is compiled to Contrail IF-MAP XSD, which is then compiled into data model CRUD APIs to be plugged into the Contrail API server. The same data model section is also compiled to the Service REST API to access the data model with ability to do paging, filtering, sorting on top of basic CRUD APIs supported by the Contrail API server. 

The non-CRUD operation section of the service YANG is compiled to REST API stubs to be implemented with service specific business logic. The business logic can access the data model via Contrail API Server and listen for data model changes via RabbitMQ. 

The YANG notification section of the service YANG is compiled into implementation that sends notification to REST client via HTML5 server sent events over HTTP.

###3. <a name="section3"></a>Contrail IF-MAP Data Model Semantics
This section intends to give an introduction to Contrail IF-MAP data model semantics. The main advantages of defining service data model with such semantics are
- The data model can be easily mapped to key-value scale-out database such as Cassandra.
- REST API (and its implementation) to access the data model can be generated from data model schema. 
- Other cross-cutting features, such as RBAC, data model change notifcation, and logging, can also be auto-generated from the data model schema.

Contrail IF-MAP data model semantics are basically the graph semantics. Three basic constructs are `identity` (graph vertex), `reference` (edge between the vertices), and `property` that can be attached to either vertex or edge of graph.

**Identity**  
An identity represents a vertex in the IF-MAP graph data model. It uniquely identify a category of objects in the data model. Each `identity` instance is uniquely identified by an UUID generated according to [RFC-4122](http://www.ietf.org/rfc/rfc4122.txt). In Contrail implementation, each identity is mapped to a table in Cassandra database.  
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/I_P.png)
    
**Reference**  
A reference represents a directional edge between two vertices in the IF-MAP graph data model. Here are three categories of references supported by Contrail:
* ***[Ref]*** - Strong reference to guarrantee referential integrity. Object referenced object can not be deleted until the reference is removed.  
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/ref_link.png)
* ***[Has]*** - Child object can not exist without parent. When parent object is deleted, its linked child objects are deleted automatically.   
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/has_link.png)
* ***[Conn]*** - Weak reference that does not prevent referenced object from being deleted. It is up to the application code to validate existence of referenced objects.   
    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/conn_link.png)  

**Property**  
The `identity` or `reference` in the IF-MAP data model can have any number of properties. Each property is a name value pair. The value can be either a primitive type or a nested data structure such as JSON. In Contrail implementation, properties are mapped to columns of the Cassandra table.  
![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/P.png)

Here is an example from Contrail data model:  
![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/vnc.png)

###4. <a name="section4"></a>Common Data Model vs Service Specific Data Model

The common data model is shared by all IQ services. "Shared" means that the schema of the common data model is shared among multiple IQ services. It should be possible for any IQ service to extend an existing identity by adding more properties or links to other identities. Identities in this model are in the same namespace to ensure that IQ services do not define their own version of "virtual-network" identity for example. The other important aspect is that the database schema and REST API to read/write persistent data are generated from the data model schema. 

An IQ service can also have its own private data model, or service specific data model. The data model schema of one IQ service may **not** be extended or changed by another IQ service. Each service specific data model has its own namespace. The API to the service specific data model should also be model-driven and generated from a modeling language. It is perfectly OK to use the same data store backend for the sevice specific model as the common data model.

###5. <a name="section5"></a>XSD as Modeling Language for IF-MAP Data Model###
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

###6. <a name="section6"></a>YANG as Modeling Language for IF-MAP Data Model###
YANG is an industry standard data model definition language that can also be used to define data model with IF-MAP semantics. We take advantage of some YANG features such as grouping, augmentation, etc to enhance the modularity, readability, and overall usability of the schema.

We defined a YANG grouping `ifmap:Idnetity` to mark a top level YANG node as an IF-MAP identity. Any direct child nodes of the identity node are defined as the IF-MAP property of the identity. We also defined 3 YANG groups (`ifmap:HasLink`, `ifmap:RefLink`, and `ifmap:ConnLink`) to mark an YANG node as an IF-MAP link. Instead of defining the IF-MAP link as a top level schema node as in the XSD representation, we define it as child node of the identity node that inherits the `ifmap:HasLink`, `ifmap:RefLink`, or `ifmap:ConnLink`. The child nodes of the link node are defined as the IF-MAP properties of the link node.

In the following example, `virtual-network` is defined as an IF-MAP identity. `allow-transit` and `network-id` are defined as the IF-MAP properties of the `virtual-network` identity. `network-policies` is defined as the IF-MAP link from `virtual-network` to `network-policy`. `sequence` is defined as the IF-MAP property of the `network-policies` link. 
```
    // IF-MAP identity
    list virtual-network {
        description "Virutal Network";
        uses ifmap:Identity;  // mark the virtual-network as an IF-MAP identity
        key uuid;  

        // IF-MAP properties for the identity
        leaf allow-transit { 
            type boolean; 
        }
        leaf network-id {
            type uint32;
            description "A unique id for the network, auto generated";
        }
        ...
   
        // IF-MAP REF links
        list network-policies {
            uses ifmap:RefLink; // mark network-policies as the RefLink from virtual-network to network-policy
            key to;
            leaf to {
                type leafref { path "/network-policy/uuid"; }
            }
            
            // IF-MAP property for the link
            container sequence {
                leaf major { type uint32; }
                leaf minor { type uint32; }
            }
            ...
        }
        ...
   }
```

**Organizing identities into YANG sub-modules**

When there are large number identities defined in the common data model, it is desirable to group the identities in separate YANG sub-modules. In practice, the sub-modules could be owned by different teams within the company. YANG's augmentation feature allows us to extend/augment an identities defined in different sub-modules.

We have a top level YANG module `iq-common-data-model` for the common data model. This module includes a list of sub-modules. Each sub-module defines a set of identities. For example, the `inv-mgt.yang` contains the `inventory-management` sub-module that defines identities such as `device`, `physical-interface`, `logical-interface`, etc. In this example, the ***img-mgt.yang*** sub-module extends the `device` identity defined in ***inv-mgt.yang*** with a new RefLink to the `image` identity.

*iq-cdm.yang*
```
module iq-common-data-model {
    yang-version 1;
    namespace "http://www.juniper.net/ns/vnc";
    prefix "iq-cdm";
    organization "Juniper Networks";
    revision "2015-02-05" { description "Initial revision"; }	
    description "Juniper Common Data Model";

    include inventory-management;
    include device-management;
    include image-management;
    include template-management;
    ...
}
```
*inv-mgt.yang*
```
submodule inventory-management {
    yang-version 1;
    belongs-to "iq-common-data-model" {
        prefix "iq-cdm";
    }

    import ietf-inet-types { prefix "inet"; }
    import ietf-yang-types { prefix "yang"; }
    import iq-ifmap-types { prefix "ifmap"; }
    contact "JUNOS Space <jspace@juniper.net>";
    organization "Juniper Networks";
    description "Inventory management";
    revision "2015-02-05" { description "Initial revision"; }

    list device {
        description "Networking device";
        uses ifmap:Identity;
        key uuid;

	...
    }
    
    ...
}
```
*img-mgt.yang*
```
submodule image-management {
    ...
    
    list image {
        description "Device image";
        uses ifmap:Identity;
        key uuid;
	
	...
    }

    augment "/device" {
        list image {
            uses ifmap:RefLink; // RefLink from device to image
            key to;
            leaf to {
                type leafref { path "/image/uuid" };
            }
            
            ...
        }
    }
    
    ...
}
```

**Defining non-CRUD service API via YANG RPC**  
(TODO)

**Defining service specific notification via YANG notification**  
(TODO)

###7. <a name="section7"></a>RESTCONF API to access the common or service-specific data model
The REST APIs to access the common or service-specific data model are generated from the service YANG according to RESTCONF spec. This section does not cover the details on RESTCONF which can be found at [http://tools.ietf.org/html/draft-ietf-netconf-restconf-04](https://tools.ietf.org/html/draft-ietf-netconf-restconf-04)). Instead, we go over an example in this section to show what the APIs look like when they are generated from service YANG.

Let's start with defining the `device` identity in common data model as following:
```
    // IF-MAP property
    list device {
        description "Networking Device";
        uses ifmap:Identity;
        key uuid;
        
        // IF-MAP properties
        container system {
            description "Device system info";
            leaf family { type string; description "Device Family";}
            leaf ip { type inet:ip-address; description "Device mgt IP"; }
        }
    }
```

**API to retrieve list of devices**  
*HTTP Request*
```
curl -XGET 'http://10.87.127.180:8082/restconf/data/iq-common-data-model:device?size=10&from=5&depth=1&show_href=true' -d '
{ 
    "filter": { "system/family": { "match" : "junos-es"} },
    "sort": [ { "name", "asc" } ]
}
'
```

*HTTP Response*
```
HTTP/1.1 200 OK
Server: nginx/1.7.9
Date: Thu, 05 Mar 2015 01:06:40 GMT
Content-Type: application/yang.data+json; charset=UTF-8
Content-Length: 19659
Connection: keep-alive

{
    @size: 2,
	"deivce": [
		{
			"name": "my-SRX-1",
			"fq-name": [ "coke-domain", "finance", "my-SRX-1"],
			"uuid": "36efa9d3-151e-4535-93da-ca6a8cc78862",
			"href": "http://10.87.127.180:8082/restconf/data/vnc-cfg/iq-common-data-model:device=36efa9d3-151e-4535-93da-ca6a8cc78862"
		}, 
		{
			"name": "my-SRX-2",
			"fq-name": [ "coke-domain", "finance", "my-SRX-2"],
			"uuid": "6554ee51-3dac-488e-8161-c083ea9d04db",
			"href": "http://10.87.127.180:8082/restconf/data/vnc-cfg/iq-common-data-model:device=6554ee51-3dac-488e-8161-c083ea9d04db"
		}
	]				
}
```

**API to retrieve device by uuid**

*HTTP Request*
```
curl -XGET 'http://10.87.127.180:8082/restconf/data/iq-common-data-model:device=36efa9d3-151e-4535-93da-ca6a8cc78862'
```

*HTTP Response*
```
HTTP/1.1 200 OK
Server: nginx/1.7.9
Date: Thu, 05 Mar 2015 01:06:40 GMT
Content-Type: application/yang.data+json; charset=UTF-8
Content-Length: 19659
Connection: keep-alive

{
	"name": "my-SRX-1",
	"fq-name": [ "coke-domain", "finance", "my-SRX-1"],
	"uuid": "36efa9d3-151e-4535-93da-ca6a8cc78862",
	"id_perms": {
	    "enable": true,
	    "description": null,
	    "created": "2014-10-31T18:38:58.860023",
	    "uuid": {
	        "uuid_mslong": 3958569321539454500,
	        "uuid_lslong": 10654050427475560000
	    },
	    "last_modified": "2014-10-31T18:38:58.860023",
	    "permissions": {
	        "owner": "cloud-admin",
	        "owner_access": 7,
	        "other_access": 7,
	        "group": "cloud-admin-group",
	        "group_access": 7
	    }
	},
	"system": {
		"family": "junos-es",
		"ip": "10.1.1.1"
	}
}
```

###8. <a name="section8"></a>RESTCONF API for operations

###9. <a name="section9"></a>YANG notifications


