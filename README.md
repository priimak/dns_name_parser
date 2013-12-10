Name
====

DNS name parser - Java utility library for parsing dns names, ip and hw addresses.

Synopsis
========

    import su.netdb.parser.*;

    Parser parser = new Parser();

    Hashtable result = parser.parse(str);

    System.out.println("string:  "+result.get("string"));
    System.out.println("hw:      "+result.get("hw"));
    System.out.println("name:    "+result.get("name"));
    System.out.println("domain:  "+result.get("domain"));
    System.out.println("ip_low:  "+result.get("ip_low"));
    System.out.println("ip_high: "+result.get("ip_high"));

Description
===========

"DNS name parser" is an utility library created to be used in a search application. Given a single input field its 
function is to differentiate between several types of possible input strings. Namely if it a dns name, IP address 
(exact, ip range or ip with wildcards) or hardware address. The result of the parsing is a Hashtable with possible 
keys "string", "hw", "name", "domain", "ip_low" and "ip_high". Here are few examples:

    input                string           name    doman     hw                ip_low               ip_high
    161.64.12-4          NULL             NULL    NULL      NULL              161.64.12.0          161.64.20.14
    171.64.20.1[01][1-9] NULL             NULL    NULL      NULL              171.64.20.1[01][1-9] NULL
    171.66.120.40/29     NULL             NULL    NULL      NULL              171.66.120.40        171.66.120.47
    171.64.20.19         NULL             NULL    NULL      NULL              171.64.20.19         171.64.20.19
    a.and.b.com          NULL             a       and.b.com NULL              NULL                 NULL
    asdj^da4.foo.com     asdj^da4.foo.com NULL    NULL      NULL              NULL                 NULL
    FF:0F:EE:0E:DD:0D    NULL             NULL    NULL      FF:0F:EE:0E:DD:0D NULL                 NULL

Complete regression test is provided in distributed package (see file test.java). Compile everything and run script 'test' in top level directory to see all possible inputs.

Requirements
============

* Java 1.5 or higher.
* Jakarta ORO.

Note that once unpacked you will probably need to modify SETTINGS file, which describes location of JDK and other settings
