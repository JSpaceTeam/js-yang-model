###Retrieve all domains with paging and content filter###

**Request**
```
URL: http://10.87.127.180:8082/restconf/data/vnc-cfg/domain?limit=2&offset=0&depth=1
Method: GET
Header:
  Host: 10.87.127.180:8082
  Accept: application/yang.data+json"
```

**Response**
```
header:
  HTTP/1.1 200 OK,
  Content-Type: application/yang.data+json

body: 
{
	"domain": [
		{
			"name": "default-domain",
			"fq-name": [ "default-domain" ],
			"uuid": "36efa9d3-151e-4535-93da-ca6a8cc78862",
			"href": "http://10.87.127.180:8082/restconf/data/vnc-cfg/domain/36efa9d3-151e-4535-93da-ca6a8cc78862"
		}, 
		{
			"name": "coke-domain",
			"fq-name": [ "default-domain" ],
			"uuid": "36efa9d3-151e-4535-93da-ca6a8cc78862",
			"href": "http://10.87.127.180:8082/restconf/data/vnc-cfg/domain/6554ee51-3dac-488e-8161-c083ea9d04db"
		},
		{
			"name": "pepsi-domain",
			"fq-name": [ "default-domain" ],
			"uuid": "36efa9d3-151e-4535-93da-ca6a8cc78862",
			"href": "http://10.87.127.180:8082/restconf/data/vnc-cfg/domain/0b34ec9f-5628-4239-9b62-0ceb9e5ac00f"
		}
	]				
}
```

###GET a specific domain###

**Reqest**
```
URL: http://10.87.127.180:8082/restconf/data/vnc-cfg/domain=36efa9d3-151e-4535-93da-ca6a8cc78862
header:
	Host: 10.87.127.180:8082
	Accept: application/yang.data+json
```

***Response***
```
header:
  HTTP/1.1 200 OK
	Content-Type: application/yang.data+json

body:
{
	"name": "default-domain",
	"fq-name": [ "default-domain" ],
	"uuid": "36efa9d3-151e-4535-93da-ca6a8cc78862",
	"href": "http://10.87.127.180:8082/domain/36efa9d3-151e-4535-93da-ca6a8cc78862",
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
	"service_templates": [
		{
			"to": [ "default-domain", "analyzer-template" ],
			"href": "http://10.87.127.180:8082/service-template/fab9191f-ba69-4394-b0a3-5d1b6bb7e1fd",
			"type": "service-template",
			"uuid": "fab9191f-ba69-4394-b0a3-5d1b6bb7e1fd"
		},
		{
		    "to": [ "default-domain", "nat-template" ],
		    "href": "http://10.87.127.180:8082/service-template/15259174-bfb5-44d7-b93a-7a160ec25c5b",
		    "uuid": "15259174-bfb5-44d7-b93a-7a160ec25c5b"
		},
		{
		    "to": [ "default-domain", "netns-snat-template" ],
		    "href": "http://10.87.127.180:8082/service-template/cecd470f-f5cc-4cd1-acd4-f8d8607333d5",
		    "uuid": "cecd470f-f5cc-4cd1-acd4-f8d8607333d5"
		},
		{
		    "to": [ "default-domain", "haproxy-loadbalancer-template" ],
		    "href": "http://10.87.127.180:8082/service-template/39d3566e-6868-48ce-a053-455bdfc19a6c",
		    "uuid": "39d3566e-6868-48ce-a053-455bdfc19a6c"
		}
	],
  "namespaces": [
      {
          "to": [ "default-domain", "default-namespace" ],
          "href": "http://10.87.127.180:8082/namespace/12a43598-bd85-4fd2-8b00-98378ca9c469",
          "uuid": "12a43598-bd85-4fd2-8b00-98378ca9c469"
      }
  ],
  "projects": [
      {
          "to": [ "default-domain", "default-project"],
          "href": "http://10.87.127.180:8082/project/3488e91f-6494-40c1-ba79-07aece435424",
          "uuid": "3488e91f-6494-40c1-ba79-07aece435424"
      },
      {
          "to": [ "default-domain", "demo" ],
          "href": "http://10.87.127.180:8082/project/d67be88a-2f99-431f-970a-d73c37b2f864",
          "uuid": "d67be88a-2f99-431f-970a-d73c37b2f864"
      }
  ]
}
```
