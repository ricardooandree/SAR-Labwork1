package com.sar;
import com.sar.format.*;
import java.net.Socket;
import java.util.Properties;
import java.util.Random;

/**
 *
 * @author pedroamaral
 */
public class JavaRESTAPI {
    private groupDB db;
     private final String bdname= "grupos.txt";
    
    /** Creates a new instance of JavaRESTAPI */
    public JavaRESTAPI () {
        db= new groupDB(bdname);
    }
    
    /** Select a subset of 'k' number of a set of numbers raging from 1 to 'max' */    
    private int[] draw_numbers(int max, int k) {
        int[] vec= new int [k];
        int j;
        
        Random rnd= new Random(System.currentTimeMillis ());
        for (int i= 0; i<k; i++) {
            do {
                vec[i]= rnd.nextInt(max)+1;
                for (j= 0; j<i; j++) {
                    if (vec[j]==vec[i])
                        break;
                }
            } while((i!=0) && (j<i));
        }
        return vec;
    }
    
    /** Selects the minimum number in the array */
    private int minimum(int[] vec, int max) {
        int min= max+1, n= -1;
        for (int i= 0; i<vec.length; i++) {
            if (vec[i]<min) {
                n= i;
                min= vec[i];
            }
        }
        if (n == -1) {
            System.err.println("Internal error in API.minimum\n");
            return max+1;
        }
        vec[n]= max+1;  // Mark position as used
        return min;
    }
    
    /** Private method returns page with "Not Implemented" */
    private void not_implemented(Response reply, String method) {
        // Define html error page
        String txt= "<HTML>\n";
        txt=txt+"<HEAD><TITLE>Error - " + method + " not implemented\n</TITLE></HEAD>\n";
        txt= txt+ "<H1> Error - " + method + " not implemented </H1>\n";
        txt= txt+ "  by JavaAPI\n";
        txt= txt + "</HTML>\n";
        // Prepare reply code and header fields
        reply.setTextHeaders(txt);
        reply.setCode(ReplyCode.NOTIMPLEMENTED);
        reply.setVersion("HTTP/1.1");
    }
    
      /** Prepares the SARAPI web page that is sent as reply to the API call */
    private String make_Page(String ip, int port, String tipo, String grupo, int n, String n1, String na1, String n2, String na2, String n3, String na3, boolean count, String lastUpdate) {
        // Draw "lucky" numbers
        int[] set1= draw_numbers(50, 5);
        int[] set2= draw_numbers(9, 2);
        
        // Prepare string html with web page
        String html= "<!doctype html>\r\n<html class\"no-js\" lang\"eng\">\r\n<head>\r\n";
        html += "<meta charset=\"utf-8\">\r\n";
        html += "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\r\n";
        html += "<title>SAR 23/24</title>\r\n";
        html += "<link rel=\"stylesheet\" href=\"css/foundation.css\" />\r\n";
        html += "<script src=\"js/modernizr.js\"></script>\r\n</head>\r\n<body>\r\n";
        html += "<div class=\"row\">\r\n<div class=\"medium-12 columns\">\r\n";
        html += "<p><img src=\"img/header.png\" /></p>\r\n</div> \r\n<div class=\"medium-12 columns\" >\r\n<div class=\"contain-to-grid\">\r\n";
        html += "<nav class=\"top-bar\" data-topbar>\r\n<ul class=\"title-area\">\r\n<li class=\"name\">\r\n<h1><a href=\"SarAPI\">S.A.R 22/23</a></h1></li>\r\n";
        html += "<li class=\"toggle-topbar menu-icon\"><a href=\"#\"><span>menu</span></a></li>\r\n</ul>";
        html += "<section class=\"top-bar-section\">\r\n<ul class=\"right\">\r\n<li><a href=\"SarAPI\">API request</a></li></ul></section></nav></div></div></div>\r\n";
        html += "<div class=\"row\">\r\n<div class=\"medium-12 columns\">\r\n<div class=\"panel\">\r\n";
        html += "<h2>SAR 2023/2024</h2>\r\n";
        html += "<p>Connection from:" + ip + ": ";
        html += "" + port + " in a browser: " + tipo + "</p>\r\n";
        if (n >= 0) {
            html += "<p>The group:" + (grupo.length()>0 ? (grupo) : "?") + " was already updated";
            html += "" + n + "times.</p>\r\n";
        }
        if (n >= 0) {
            html += "<p align=\"left\">O last acces to this server by this user was in: " +
                    " <font color=\"#0000ff\">" + lastUpdate + "</font>.</p>\r\n";
        }
        html += "<form method=\"post\" action=\"sarAPI\">\r\n<h3>\r\nDados do grupo</h3>";
        html += "<p>Group <input name=\"Grupo\" size=\"2\" type=\"text\""+
                (grupo.length()>0 ? " value=\""+grupo+"\"": "")+"></p>\r\n";
        html += "<p>Number <input name=\"Num1\" size=\"5\" type=\"text\""+
                (n1.length()>0 ? " value="+n1 : "")+"></p>\r\n"+
                "Name <input name=\"Nome1\" size=\"80\" type=\"text\""+
                (na1.length()>0 ? " value="+na1 : "")+
                "></p>\r\n";
        html += "<p>Number <input name=\"Num2\" size=\"5\" type=\"text\""+
                (n2.length()>0 ? " value="+n2 : "")+"></p>\r\n"+
                "Name <input name=\"Nome2\" size=\"80\" type=\"text\""+
                (na2.length()>0 ? " value="+na2 : "")+
                "></p>\r\n";
        html += "<p>Number <input name=\"Num3\" size=\"5\" type=\"text\""+
                (n3.length()>0 ? " value="+n3 : "")+"></p>\r\n"+
                "Name <input name=\"Nome3\" size=\"80\" type=\"text\""+
                (na3.length()>0 ? " value="+na3 : "")+
                "></p>\r\n";
        html += "<p><input name=\"Contador\"" + (count?" checked=\"checked\"":"") + " value=\"ON\" type=\"checkbox\">Contador</p>\r\n";
        html += "<p><input value=\"Submeter\" name=\"BotaoSubmeter\" type=\"submit\">";
        html +=     "<input value=\"Apagar\" name=\"BotaoApagar\" type=\"submit\">";
        html +=     "<input value=\"Limpar\" type=\"reset\" value=\"Reset\" name=\"BotaoLimpar\">";
        html +=     "</p>\r\n</form>\r\n";
        html += "<h3>Registered groups</h3>";
        html += db.table_group_html();
        html += "<h3>Example of dynamic content :-)</h3>";
        html += "<p align=\"left\">If you want to waste some money, here are some suggestions for the next ";
        html += "<a href=\"https://www.jogossantacasa.pt/web/JogarEuromilhoes/?\">Euromillions</a>: ";
        for (int i= 0; i<5; i++)
            html += (i==0 ? "" : " ")+ "<font color=\"#00ff00\">"+minimum(set1, 50)+"</font>";
        html += " + <font color=\"#800000\">"+minimum(set2, 9)+"</font> <font color=\"#800000\">"+minimum(set2, 9)+"</font></p>\r\n";
        html += "<p align=\"left\">&nbsp;</p>\r\n";
        html += "</div>\r\n</div>\r\n";
        html += "<footer class=\"row\">\r\n<div class=\"medium-12 columns\">\r\n<hr />\r\n<p>© DEE - FCT/UNL.</p>\r\n</div>\r\n</div>\r\n</footer>\r\n";
        html += "<script src=\"js/jquery.js\"></script>\r\n";
        html += "<script src=\"js/foundation.min.js\"></script>\r\n";
        html += "<script src=\"js/foundation/foundation.topbar.js\"></script>\r\n";
        html += "<script>\r\n$(document).foundation();\r\n</script>\r\n</body>\r\n</html>\r\n";
        
        return html; // HTML page code
    }

            
    /** Runs GET method */
    public boolean doGet(Socket s, Headers headers, Properties cookies, Response reply) { 
      System.out.println("run API GET");

        String group="", nam1="", n1="", nam2="", n2="", nam3="", n3="", lastUpdate="";
        int cnt= -1;
        /**
         *      This part must check if the browser is sending the sarCookie
         *      If it is, it must deliver a web page with the last group introduced by the user
         *      Otherwise, the fields must be empty
         */
        System.out.println("Cookies ignored in API GET");

        // Prepare html page
        String html= make_Page(s.getInetAddress().getHostAddress(), s.getPort(), headers.getHeaderValue("User-Agent"), 
                group, cnt, n1, nam1, n2, nam2, n3, nam3, false, lastUpdate);

        // Prepare answer
        reply.setCode(ReplyCode.OK);
        reply.setTextHeaders(html); //sets text headers. 
        // Complete the code. Add the missing header fields!
        System.out.println("Missing header fields in API GET");
        return true;
    }
    
    /** Runs POST method */
    public boolean doPost(Socket s, Properties cookies, Headers headers, Properties fields, Response reply) { 
        // Put POST implementation here
        System.out.println("run API POST");
        String group= fields.getProperty("Grupo", "");
        String nam1= fields.getProperty("Nome1", "");
        String n1= fields.getProperty("Num1", "");
        String nam2= fields.getProperty("Nome2", "");
        String n2= fields.getProperty("Num2", "");
        String nam3= fields.getProperty("Nome3", "");
        String n3= fields.getProperty("Num3", "");
        boolean SubmitButton= (fields.getProperty("BotaoSubmeter")!=null);
        boolean DeleteButton= (fields.getProperty("BotaoApagar")!=null);
        String lastUpdate="";
        
        System.err.println("Button: "+(SubmitButton?"Submit":"")
                +(DeleteButton?"Delete":"")+"\n");
        int cnt= -1;

        System.out.println("Command not implemented in API POST");
                
        // Prepare html page
        String html= make_Page(s.getInetAddress().getHostAddress(), s.getPort(), headers.getHeaderValue("User-Agent"), 
                group, cnt, n1, nam1, n2, nam2, n3, nam3, (fields.getProperty("Contador") != null), lastUpdate);

        // Prepare answer
        reply.setCode(ReplyCode.OK);
        reply.setTextHeaders(html);
        // Complete the code. Add the missing header fieds!
        System.out.println("Missing header fields in API POST");
        return true;
    }
    
}
