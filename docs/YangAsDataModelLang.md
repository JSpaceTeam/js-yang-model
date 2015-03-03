### YANG as DDL for JUNOS-IQ Services
  

###Introduction###
We have chosen YANG as the primary modeling and API definition language for config data in JUNOS IQ. Here is diagram from Bruno's [iq-platform-architecture-specification](https://junipernetworks.sharepoint.com/teams/cto/JunosIQ/JunosIQArch/docs/iq-platform-architecture-specification---22-dec-2014---v8.docx):
![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/DataModelDrivenInterface.png)

This document focuses on using YANG to define data model for the IQ services with IF-MAP semantics. 

###Contrail IF-MAP Data Model Semantics###
Contrail data model follows IF-MAP graph-based data model semantics. Data model defined with such semantics can be easily mapped to key-value scale-out database such as Cassandra. The database schema, REST API, and code to read/write the persisted data model, can be generated from the modeling language. Implementation of other cross-cutting features, such as RBAC, notifcation, and logging etc, can also be generated automatically from the data model schema.

Here are three basic constructs of IF-MAP based data model: Identity, Reference and Property

**Identity**  
* Node in the IF-MAP data model graph that represents a type of objects  
* Identity can be attached with one or more properties  

    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/I_P.png)

* Each instance of a certain identity is identified by an UUID generated according to RFC-4122  
* Identities are exposed as REST resources when accessed via REST API
* Each identity are mapped to a table in Cassandra database.

**Reference**

There are three types of references  
* ***[Ref]*** - Strong reference to guarrantee referential integrity. Object referenced object can not be deleted until the reference is removed

    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/ref_link.png)

* ***[Has]*** - Child object can not exist without parent. When parent object is deleted, its linked child objects are deleted automatically.   

    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/has_link.png)

* ***[Conn]*** - Weak reference that does not prevent referenced object from being deleted. It is up to the application code to validate existence of referenced objects.

    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/conn_link.png)  

**Property**  
* One or more properties can be attached to either Identities or References  

    ![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/P.png)

* Property value can be either of primitive types or nested data structure such as JSON  
* Properties are mapped to columns of a Cassandra table.

Here is an example from Contrail data model:

![](https://github.com/JSpaceTeam/JSpaceTeam.github.io/raw/master/images/js-yang-model/vnc.png)

###Common Data Model vs Service Specific Data Model###

The common data model is shared by all IQ services. "Shared" means that the schema of the common data model is shared among multiple IQ services. It should be possible for any IQ service to extend an existing identity by adding more properties or links to other identities. Identities in this model are in the same namespace to ensure that IQ services do not define their own version of "virtual-network" identity for example. The other important aspect is that the database schema and REST API to read/write persistent data are generated from the data model schema. 

An IQ service can also have its own private data model, or service specific data model. The data model schema of one IQ service may **not** be extended or changed by another IQ service. Each service specific data model has its own namespace. The API to the service specific data model should also be model-driven and generated from a modeling language. It is perfectly OK to use the same data store backend for the sevice specific model as the common data model.

###XSD as Modeling Language for IF-MAP Data Model###
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

Here is an example of defining properties for the `virtual-network` identity. 
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

Here is an example of defining a link between virtual-network and network policy. Note that `sequence` and `timer` are the properties for the `virtual-network-network-policy` link.
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

###YANG as Modeling Language for IF-MAP Data Model###
YANG is an industry standard data model definition language that can also be used to define data model with IF-MAP semantics. We take advantage of some YANG features such as grouping, augmentation, etc to enhance the usability and readability of the schema.

Any top level YANG node that uses grouping `ifmap:Identity` is the definition for an IF-MAP identity. Any direct child node of the identity node that does not uses `ifmap:HasLink` or `ifmap:RefLink` is the definition for a property of the identity. The key difference of YANG representation from the XSD representation is that IF-MAP properties and Links to other identities are orangized hierachically inside the identity node.

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
        leaf vxlan-network-identifier {
            type uint32;
            range "0 .. 1048575";
            description "VNI for the network, configured by user";
        }
        leaf forwarding-mode {
            type enumeration { 
                enum l2_l3;
                enum l2;
            }
            description "Forwarding mode for virtual-network";
        }
   
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

When there are large number identities defined in the common data model, it is desirable to group the identities in separate YANG sub-modules. In practice, the sub-modules could be owned by different teams within the company. YANG's augmentation feature makes it handy for one sub-module to extend or augment an identities defined in another sub-module.

We have a top level YANG module `iq-common-data-model` defined in `iq-cdm.yang". This module includes a list of sub-modules. 
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

Each sub-module defines a set of identities. For example, the `inv-mgt.yang` contains the `inventory-management` sub-module that defines identities such as `device`, `physical-interface`, `logical-interface`, etc. 
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

Other sub-modules can extend an existing identity defined in a different module via YANG augmentation. In the following example, the `image-management` sub-module extends the `device` by adding a new RefLink to the `image` identity.
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

###Non-CRUD operation###

###Data Model Change Notification##

###REST API for CRUD operations###

###Map YANG RPC to REST###

###Map YANG notification to REST###


