/* $Id: Parser.java,v 1.9 2004/11/27 07:43:38 priimak Exp $
 * $Name: v1_2_1 $
 */
package su.netdb.parser;
import java.lang.*;
import java.util.*;
import org.apache.oro.text.regex.*;

/**
 * This class provides parser originally intended to be used
 * in whois and netdb to parse input string and classify it
 * as a <b>name</b>, <b>hw</b>, <b>string</b>, <b>ip</b>,
 * <b>ip range</b> or <b>ip regex</b> or throws error on illigal string.
 * Note that you need package org.apache.oro from
 * <a href="http://jakarta.apache.org/oro">http://jakarta.apache.org/oro</a> to compile
 * and use this class, see README under docs/
 */
public class Parser {
    /**
     * When set to <i>true</i> parser prints to stdout some info during parsing.
     */
    public boolean debug = false;

    private static PatternCompiler pcompiler = new Perl5Compiler();
    private static PatternMatcher pmatcher = new Perl5Matcher();
    private static Pattern pat[] = {null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null,
                        null, null, null, null, null};

    public Parser() {
     if( pat[0] != null ) return;
     try {
         pat[0] = pcompiler.compile("([-0-9\\[\\]\\/%*?_.]+)\\s*-\\s*([-0-9\\[\\]\\/%*?_.]+)");
         pat[1] = pcompiler.compile("\\s+");
         pat[2] = pcompiler.compile("^[a-fA-F0-9]{1,2}(:[a-fA-F0-9]{1,2}){5}$"); // 08:00:20:85:8b:0f
         pat[3] = pcompiler.compile("^[a-fA-F0-9]{1,2}(-[a-fA-F0-9]{1,2}){5}$"); // 08-00-20-85-8b-0f
         pat[4] = pcompiler.compile("^[a-fA-F0-9]{4}(\\.[a-fA-F0-9]{4}){2}$"); // 0800.2085.8b0f
         pat[5] = pcompiler.compile("^[a-fA-F0-9]{6}:[a-fA-F0-9]{6}$"); // 080020:858b0f
         pat[6] = pcompiler.compile("^[a-fA-F0-9]{12}:$"); // 080020858b0f:
         pat[7] = pcompiler.compile("^[a-fA-F0-9]{12}$"); // 080020858b0f
         pat[8] = pcompiler.compile("^\\d{1,3}\\.[-0-9\\[\\]\\/%*?_.^]*$"); // IP address?
         pat[9] = pcompiler.compile("^[-a-zA-Z0-9\\^\\[\\].%*_?]+$"); // DNS name?
         pat[10] = pcompiler.compile(":"); // split pattern
         pat[11] = pcompiler.compile("-"); // split pattern
         pat[12] = pcompiler.compile(".*\\/.*");
         pat[13] = pcompiler.compile(".*[][*%_?^].*");
         pat[14] = pcompiler.compile("\\[.*?\\]");
         pat[15] = pcompiler.compile("\\s*-\\s*");
         pat[16] = pcompiler.compile("(.*\\.)(.*)");
         pat[17] = pcompiler.compile("(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)");
         pat[18] = pcompiler.compile(".*[][*%_?-].*");
         pat[19] = pcompiler.compile(".*[^[]\\^.*"); // check valid combination [^
         pat[20] = pcompiler.compile(".*\\.\\..*"); // two dots in ip -> invalid ip

         // ip address may not contain more then tree dots, if it matches below it is wrong
         pat[21] = pcompiler.compile(".*\\..*\\..*\\..*\\..*");

         pat[22] = pcompiler.compile("^[a-fA-F0-9]{1,2}(\\s+[a-fA-F0-9]{1,2}){5}$"); // 08 00 20 85 8b 0f
         pat[23] = pcompiler.compile("\\s+"); // split pattern
     } catch(Exception ex) {} // there shouldn't be any exception
    }

    /**
     * Actuall parsing happens here.
     * @param src  Input string
     * @return     Hashtable where forllowing keys are possible <b>string</b>,
     * <b>hw</b> - hardware address, <b>name</b> - dns name,
     * <b>domain</b> - when src is a FQDN than src got splitted into two parts <b>name</b> and <b>domain</b>,
     * <b>ip_low</b> and <b>ip_high</b> - when ip_low == ip_high then it correspond to sigle <i>ip</i>,
     * when ip_low < ip_high then it is an <i>ip range</i>, when ip_low exists and ip_high doesn't then
     * it is <i>ip regex</i>
     * @exception  In case when error encountered during parsing Exception will be thrown with one of the
     * following messages: '<b>bad IP address expression</b>', '<b>bad IP range, low > high</b>',
     * '<b>bad prefix length, ...</b>' - where ... correspond to number which is a prefix length,
     * '<b>internall error 001.</b>' , '<b>internall error 002.</b>', '<b>internall error 003.</b>', last three
     * exceptions 'internal error' should not ever appear and correspond to the bug(s) in parser.
     */
    public Hashtable parse(String src) throws Exception {
     src = src.trim();
     String src_orig = new String(src);
     Hashtable result = new Hashtable();

     if( pmatcher.matches(src, pat[0]) )
         src = Util.substitute(pmatcher, pat[0],
                      new Perl5Substitution("$1-$2"),
                      src, Util.SUBSTITUTE_ALL);

     List hw = new ArrayList(); // here is gonna be hardware address

     // if there are spaces, it's not an address or hostname
     if( pmatcher.contains(src, pat[1]) ) {
         src = src.replace('*', '%');
         src = src.replace('?', '_');

         if( pmatcher.matches(src, pat[22]) ){ // 08 00 20 85 8b 0f
             Util.split(hw, pmatcher, pat[23], src);
             String str = "";
             Iterator it = hw.iterator();
             String hw_element = "";
             while( it.hasNext() ) {
                 hw_element = (String)it.next();
                 if( hw_element.length() == 1 )
                     str += "0"+hw_element;
                 else
                     str += hw_element;
             }
             result.put("hw", str);
         }
         result.put("string", src);
         return result;
     }

     boolean got_hw = false;
     boolean got_name = false;

     if(       pmatcher.matches(src, pat[2]) ){ // 08:00:20:85:8b:0f
         Util.split(hw, pmatcher, pat[10], src);
         got_hw = true;

     }else if( pmatcher.matches(src, pat[3]) ){ // 08-00-20-85-8b-0f
         Util.split(hw, pmatcher, pat[11], src);
         got_hw = true;
         got_name = true;

     }else if( pmatcher.matches(src, pat[4]) ){ // 0800.2085.8b0f
         hw.add(src.substring(0, 2));
         hw.add(src.substring(2, 4));
         hw.add(src.substring(5, 7));
         hw.add(src.substring(7, 9));
         hw.add(src.substring(10, 12));
         hw.add(src.substring(12, 14));
         got_hw = true;
         got_name = true;

     }else if( pmatcher.matches(src, pat[5]) ){ // 080020:858b0f
         hw.add(src.substring(0, 2));
         hw.add(src.substring(2, 4));
         hw.add(src.substring(4, 6));
         hw.add(src.substring(7, 9));
         hw.add(src.substring(9, 11));
         hw.add(src.substring(11, 13));
         got_hw = true;

     }else if( pmatcher.matches(src, pat[6]) ){ // 080020858b0f:
         hw.add(src.substring(0, 2));
         hw.add(src.substring(2, 4));
         hw.add(src.substring(4, 6));
         hw.add(src.substring(6, 8));
         hw.add(src.substring(8, 10));
         hw.add(src.substring(10, 12));
         got_hw = true;

     }else if( pmatcher.matches(src, pat[7]) ){ // 080020858b0f
         hw.add(src.substring(0, 2));
         hw.add(src.substring(2, 4));
         hw.add(src.substring(4, 6));
         hw.add(src.substring(6, 8));
         hw.add(src.substring(8, 10));
         hw.add(src.substring(10, 12));
         got_hw = true;
         got_name = true;
     }

     if( got_hw ){
         String str = "";
         Iterator it = hw.iterator();
         String hw_element = "";
         while( it.hasNext() ) {
          hw_element = (String)it.next();
          if( hw_element.length() == 1 )
              str += "0"+hw_element;
          else
              str += hw_element;
         }
         result.put("hw", str);
         if( got_name )
          result.put("name", src_orig);
         return result;
     }

     boolean got_ip = false;
     boolean got_dnsname = false;

     if(      pmatcher.matches(src, pat[8]) ) // IP Address?
         got_ip = true;
     else if( pmatcher.matches(src, pat[9]) && !pmatcher.matches(src, pat[19]) ) // DNS Name?
         got_dnsname = true;
     else { // String
         src = src.replace('*', '%');
         src = src.replace('?', '_');
         result.put("string", src);
         return result;
     }

     // we are here if either got_dnsname or got_ip are true;
     src = src.replace('*', '%');
     src = src.replace('?', '_');

     if( got_dnsname ){
         // now try to split name and domain
         int index = src.indexOf('.');
         result.put("original_name", new String(src));
         String name = "";
         String domain = "";
         if( index != -1 ){
          // not split
          name = src.substring(0, index);
          domain = src.substring(index+1, src.length());
         }else
          name = src;
         //System.out.println("name["+name+"] domain["+domain+"]");
         result.put("name", name);
         if( domain != null && domain.length() > 0 )
          result.put("domain", domain);

     }else{ // got_ip, now do some magic
         if( ( pmatcher.matches(src, pat[12]) && pmatcher.matches(src, pat[18]) ) ||
          pmatcher.matches(src, pat[20]) )
          throw new Exception("bad IP address expression");

         String squish = Util.substitute(pmatcher, pat[14],
                             new Perl5Substitution(""),
                             src, Util.SUBSTITUTE_ALL);
         if( squish.indexOf('-') > -1 ){
          if( pmatcher.matches(src, pat[13]) )
               throw new Exception("bad IP address expression");
          List ips = new ArrayList(); // here will be ip_low and ip_high
          Util.split(ips, pmatcher, pat[11], squish);
          String ip_low = null;
          String ip_high = null;
          try{ // ips now should have two elements ip_low and ip_high
              ip_low = (String)ips.get(0);
              ip_high = (String)ips.get(1);
          }catch(Exception ex){
              throw new Exception("internall error 001.");
          }
          if( pmatcher.matches(ip_low, pat[21]) ||
              pmatcher.matches(ip_high, pat[21]) )
              throw new Exception("bad IP address expression");
          while( ip_high.indexOf('.') == -1 ){
              if( !pmatcher.matches(ip_low, pat[16]) )
               throw new Exception("internall error 002.");
              try{
               MatchResult mr = pmatcher.getMatch();
               String net = mr.group(1);
               String base = mr.group(2);
               int delta = base.length() - ip_high.length();

               if( delta > 0 )
                   ip_high = base.substring(0, delta)+ip_high;
               ip_high = net + ip_high;
              }catch(Exception ex){
               throw new Exception("internal error 003.");
              }
          }

          // now normilize ip_low and ip_high
          ip_low = ipnorm(ip_low);
          ip_high = ipnorm(ip_high);
          long ipLow = 0;
          long ipHigh = 0;
          ipLow = ipstr2num(ip_low);
          ipHigh = ipstr2num(ip_high);

          if( ipLow > ipHigh )
               throw new Exception("bad IP range, low > high");

          result.put("ip_low", ip_low);
          result.put("ip_high", ip_high);
          if( debug )
              System.out.println("ip ["+ip_low+"] - ["+ip_high+"]");
          return result;
         }
         if( pmatcher.matches(src, pat[13])  ){
          if( pmatcher.matches(src, pat[19]) ||
              pmatcher.matches(src, pat[21]) )
              result.put("string", src);
          else {
              result.put("ip_low", src);
              if( debug )
               System.out.println("ip ["+src+"] - [null]");
          }
          return result;
         } else if( pmatcher.matches(src, pat[21]) )
          throw new Exception("bad IP address expression");

         /* what is left by now should be an ip address possibly
            with a slash in it. */
         int index = src.indexOf('/');
         String ip = index==-1?src:src.substring(0, index);
         String length = index==-1?"32":src.substring(index+1);
         // normalize 'ip'
         if( ip.charAt(ip.length()-1) == '.' )
          ip += "0";
         int dots = 0; // dot's counter
         for( int i=0; i<ip.length(); i++ )
          if( ip.charAt(i) == '.' )
              dots++;
         dots = 3 - dots; // now it is amout of dots to add
         for( int i=0; i<dots; i++ )
          ip += ".0";
         ipstr2num(ip); // just for checking

         int net_length;
         net_length = Integer.parseInt(length);
         if( net_length < 8 || net_length > 32 )
          throw new Exception("bad prefix length, "+length);

         long bitmask = 0xFFFFFFFF << ( 32 - net_length );
         if( debug ){ // print bitmask in a binary
          long mask = 0x00000001;
          System.out.print("bitmask [");
          for( int i=0;i<32; i++){
              System.out.print(((bitmask&mask)==0?0:1));
              mask = mask << 1;
          }
          System.out.println("] net_length = "+net_length);
         }
         String ip_low = ipnum2str(ipstr2num(ip) & bitmask);
         String ip_high = ipnum2str(ipstr2num(ip) | ~bitmask);
         result.put("ip_low", ip_low);
         result.put("ip_high", ip_high);
         if( debug )
          System.out.println("ip ["+ip_low+"] - ["+ip_high+"]");
         return result;
     }
     return result;
    } // end of parse(...)

    /**
     * Converts ip string, regex /(\d+)\.(\d+)\.(\d+)\.(\d+)/
     * into long integer.
     */
    static public long ipstr2num(String ip) throws Exception
    {
     if( pat[0] == null )
         new Parser(); // this will ensure that patterns initialized
     if( !pmatcher.matches(ip, pat[17]) )
         throw new Exception("String passed to ipstr2num(...) is not an ip address.");
     MatchResult mr = pmatcher.getMatch();
     long num[] = new long[4];
     for( int i=0; i<4; i++ ){
         num[i] = Long.parseLong(mr.group(i+1));
         if( num[i] > 255 )
          throw new Exception("bad octet value, "+ip);
     }
     return (num[0] << 24) + (num[1] << 16) + (num[2] << 8) + num[3];
    } // end of ipstr2num(...)

    /**
     * Converts long int to string representation of ip address.
     */
    static public String ipnum2str(long ip){
     return new String(Long.toString((ip>>24) & 0xFF)+"."+
                 Long.toString((ip>>16) & 0xFF)+"."+
                 Long.toString((ip>>8) & 0xFF)+"."+
                 Long.toString(ip & 0xFF));
    }

    /**
     * Used to normilize ip, i.e. add missing .0's. Like for example
     * string 171.64.20 will return 171.64.20.0, 171.64 -> 171.64.0.0
     * Note that input string is not validated to be an ip address.
     */
    static public String ipnorm(String ip){
     String result = new String(ip);
     if( result.charAt(result.length()-1) == '.' ) // '.' at the end
         result += "0";
     int dots = 0; // dot's counter
     for( int i=0; i<result.length(); i++ )
         if( result.charAt(i) == '.' )
          dots++;
     dots = 3 - dots; // now it is amout of dots to add
     for( int i=0; i<dots; i++ )
         result += ".0";
     return result;
    } // end of ipnorm(...)

}
