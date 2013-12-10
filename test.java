/* $Id: test.java,v 1.5 2007/07/13 21:04:37 priimak Exp $ */
import java.lang.*;
import java.util.*;
import java.io.*;
import su.netdb.parser.*;

public class test {
    public test() {}

    public static void main(String arg[]) {
     Vector input = new Vector();
     Parser parser = new Parser();
     Hashtable result = null;

     if( arg.length == 0 ) {
         // read file
         String line = null;
         boolean load = false;
         try{
          BufferedReader in = new BufferedReader(new FileReader("test.java"));
          for(;;)
              if( (line = in.readLine()) != null ) {
               if( line.trim().equals("BEGIN TEST") ){
                   load = true;
                   continue;
               }
               if( line.trim().equals("END TEST") ) break;
               if( load )
                   input.add(line.trim());
              } else
               break;
         }catch(Exception ex){
          System.out.println(ex);
          System.exit(1);
         }
     } else if( arg[0].equals("-d") ) {
         try {
          input.addElement((new BufferedReader(new InputStreamReader(System.in))).readLine());
         } catch(Exception ex) {
          System.out.println(ex);
          return;
         }
     } else
         input.addElement(arg[0]);


     // loop over input strings
     StringBuffer input_str = null;
     for( int i=0; i<input.size(); i++ ) {
         try {
          input_str = new StringBuffer((String)input.elementAt(i));
          result = parser.parse((String)input.elementAt(i));

          if( result.get("hw") != null )
              System.out.println(formatString(input_str, 30, 30, '\0').toString() +
                           " : hw         " + result.get("hw"));

          if( result.get("name") != null ) {
              System.out.print(formatString(input_str, 30, 30, '\0').toString() +
                         " : name       " +
                         result.get("name"));
              if( result.get("domain") != null )
               System.out.println("."+result.get("domain"));
              else
               System.out.println("");
          }

          if( result.get("string") != null )
              System.out.println(formatString(input_str, 30, 30, '\0').toString() +
                           " : string     " + result.get("string"));

          if( result.get("ip_low") != null && result.get("ip_high") != null ) {
              if( ((String)result.get("ip_high")).equals((String)result.get("ip_low")) )
               System.out.println(formatString(input_str, 30, 30, '\0').toString() +
                            " : ip         " + result.get("ip_low") );
              else
               System.out.println(formatString(input_str, 30, 30, '\0').toString() +
                            " : range      " + result.get("ip_low") + " -- "+
                            result.get("ip_high"));
          }

          if( result.get("ip_low") != null && result.get("ip_high") == null )
              System.out.println(formatString(input_str, 30, 30, '\0').toString() +
                           " : ipregexp   " + result.get("ip_low"));

         } catch(Exception ex){
          System.out.println(formatString(input_str, 30, 30, '\0') +
                       " : error      " + ex.getMessage());
         }
     }
    }

    public static StringBuffer formatString(StringBuffer src,
                         int min_length,
                         int max_length,
                         char last) {
     if( src == null )
         return new StringBuffer("");

     int strlen = src.length();
     if( ( min_length < 1 && max_length < 1 ) ||
         ( min_length > 0 && max_length > 0 && strlen <= max_length && strlen >= min_length) ||
         ( min_length < 1 && max_length > 0 && strlen <= max_length ) ||
         ( min_length > 0 && max_length < 1 && strlen >= min_length ) ){
         if( last != 0 )
          if( max_length == strlen ){
              src.setLength(max_length-4);
              return src.append("...").append(last);
          }else
              return src.append(last);
         return src;
     }

     // at this point we know that 'src' needs some adjustments before returning it.
     String result = "";
     if( min_length > 0 && strlen < min_length ){
         int added_length = min_length - strlen;
         char[] addition = new char[added_length];
         for( int i=0; i<added_length; i++ )
          addition[i] = ' ';
         if( last != 0 )
          addition[0] = last;
         return src.append(addition);
     }else // max_length > 0 && strlen > max_length
         if( last != 0 ){
          src.setLength(max_length-4);
          return src.append("...").append(last);
         }else{
          src.setLength(max_length-3);
          return src.append("...");
         }
    } // end of formatString(...)

}

/*
BEGIN TEST
171.64.20/0
171.64.20/33
171.64.20.7/24
171.64.20/25
171.64.20
171.64.20.
171.64.20.0
171.64.20.0.9
171.64.20.0.9.7
171.64.20.*
171.64.106.0/23
171.64.107/23
171.64.20.1-171.64.20.100
171.64.20.1 -171.64.20.100
171.64.20.1- 171.64.20.100
171.64.20.1 - 171.64.20.100
171.64.20.100-171.64.20.1
171.66.120.40/29
171.64.20.[1-9]?
171.64.20.1[01][1-9]
171.64.20.1[0^1][^1-9]
171.64.20.1[^01][1-9]
171.64.20.12[0-7]
171.64.20.*
172.24.20.*
171.64.*.1
171.65.*.*
171.65.*
171.64.[3-6]/24
171.64.[1-9] - 171.64.12.0
171.64.12.0 - 171.64.[1-9]
171.64.20.0-255/24
161.64.12.1-10
161.64.12.45-43
161.64.12.42-43
161.64.20.0-9
161.64.20.10-9
161.64.20.100-9
161.64.20.12-4
161.64.12-4
161.64.^12-4
171.64.20-171.64.21
171.64.20./23
171.64.300.12
171.64.300/23
171.74.20/*
171.64.20.* - 171.64.21.*
08:00:20:85:8b:0f
08:0:20:85:8b:f
08-00-20-85-8b-0f
08 00 20 85 8b 0f
8-00-20-85-8b-f
8 00 20 85 8b f
0800.2085.8b0f
080020:858b0f
080020858b0f:
080020858b0f
00:20:85:8b:0f
08:0:85:8b:f
08-00--85-8b-0f
8-00-25-8b-f
0800.285.8b0f
08020:858b0f
0800858b0f:
08 00 85 8b 0f
0800858b0f
8 0 85  8b f
02:08:00:20:85:8b:0f
02:08:0:20:85:8b:f
02-08-00-20-85-8b-0f
02-8-00-20-85-8b-f
0204.0800.2085.8b0f
02080020:858b0f
02080020858b0f:
02080020858b0f
astro/aero
crafts - arts
crafts -&- arts dpt / usa
dmitri priimak
r.riepel
s yang
171.64.*
z-ath%.
dothey.fof
rob.riepel
za*
za*.
171.%
www.highwire.org
mary.orG
.edu
.
171.foo
36.*
rob.ri.ep.el.edu
171.%
rob.riepel@stanford.edu
a[sS]tro/aero
dm[aeiou]tri priimak
r.r[ie][ie]pel
[sS] yang
z-[a-z]ath%.
dot[ht]ey.fof
rob.r[ie]pple
z[aeiou]*
z[aeiou]*.
www.h[iy]ghwire.org
mar[iy].orG
.e[dp]u
rob.ri.[aeiou]p.el.edu
rob.r[ie][ie]pel@mit.edu
rob.ri.[^aeiou]p.el.edu
rob.ri.[^aeiou]p.e[l^a].edu
asdj^da4.foo.com
FF:0F:EE:0E:DD:0D
END TEST
*/
