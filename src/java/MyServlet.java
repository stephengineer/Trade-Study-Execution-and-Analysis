/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
 
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
/**
 *
 * @author J71860
 */

@WebServlet("/MyServlet")
public class MyServlet extends HttpServlet {
 
    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        String serverInfo=request.getSession().getServletContext().getServerInfo();
        System.out.println("Server Info" + serverInfo);
        String servletInfo= request.getSession().getServletContext().getMajorVersion() + "." +  request.getSession().getServletContext().getMinorVersion();
        System.out.println("Servlet Info" + servletInfo);
        // read form fields
        int m = Integer.parseInt(request.getParameter("numParams")); // m is short for numbers of parameters
        int n = Integer.parseInt(request.getParameter("numOpts")); // n is short for numbers of options
        
        
        // 1st step: trade study information
        String[] info = new String[4];
        info[0] = request.getParameter("tsName"); // Trade Study Name
        info[1] = request.getParameter("programName"); // Program Name
        info[2] = request.getParameter("topicName"); // Topic Name
        info[3] = request.getParameter("author"); // Author
        
        
        // 2nd step: Information about parameters
        // the (i+1)th parameters' name will be paramNames[i], i in [0, m], same with type, preference and comparison

        String[][] param = new String[m][4];
        for(int i=0; i<m; i++) {
            param[i][0]= request.getParameter("paramName"+i); // parameters names
            param[i][1] = request.getParameter("type"+i); // parameters types
            param[i][2]= request.getParameter("pref"+i); // parameters preference
            param[i][3]= request.getParameter("comp"+i); // parameters comparison
        }
        
        // 3rd step: Names about options
        String[] optNames = new String[n];
        for(int i=0; i<n; i++) {
            optNames[i]=request.getParameter("optName"+i);
        }
        
        
        // 4th step: Raw score matrix
        // ith parameter reltive to jth option raw score is rawScore[i][j]
        double[][] R = new double[m][n];
        for(int i=0; i<m; i++) {
            for(int j=0; j<n; j++) {
                R[i][j] = Double.parseDouble(request.getParameter("p"+(i+1)+"o"+(j+1)));
            }
        }
        
        
        
        /**************************************************************************************************************
         * Initialization
         ***************************************************************************************************************/
        
        int optimalOption=0; // the best option
        //  Parameter Weight matrix, P[m][m]
        double[][] P = new double[m][m];
        // Weight matrix after normalizion
        double[] parameterWeights = new double[m];
        // Option Weight Matrices respect parameter k, O[n][n][m]
        double[][][] O = new double[n][n][m];
        // the weight of option
        double[][] optionWeights = new double[n][m];
        // Utility Score for each option
        double[] S = new double[n];
        // confidence scores, SDR[m][n-1]
        double[][] SDR = new double [m][n-1];
        // average of SDR
        double aveSDR=0;
        

        /***************************************************************************************************************
         *First step: Parameter Weight matrix, P[m][m]
         ***************************************************************************************************************/
        
        // a. R is user input raw data, matrix R[m][n]
        
        // b. The options are: equally, moderately, greatly, extremely, respect to weights are: 1.0, 1.2,1.5,2.0
        for(int i=0; i<m-1; i++) {
            if("extremely".equals(param[i][3])) {
                P[i][i]=1.0;
                P[i][i+1]=2.0;
                P[i+1][i]=1.0/P[i][i+1];
            }
            else if("greatly".equals(param[i][3])) {
                P[i][i]=1.0;
                P[i][i+1]=1.5;
                P[i+1][i]=1.0/P[i][i+1];
            }
            else if("moderately".equals(param[i][3])) {
                P[i][i]=1.0;
                P[i][i+1]=1.2;
                P[i+1][i]=1.0/P[i][i+1];
            }
            else {
                P[i][i]=1.0;
                P[i][i+1]=1.0;
                P[i+1][i]=1.0/P[i][i+1];
            }
            
        }
        P[m-1][m-1]=1.0;
        
        // c.	Build up a consistent matrix. Transform matrix using the inverse function
        for(int i=0; i<m-2; i++) {
            for(int j=i+2; j<m; j++) {
                P[i][j]=P[i][j-1]*P[j-1][j];
                P[j][i]=1.0/P[i][j];
            }
        }
        
        // d.	Normalize elements in P dividing by the summation of column
        double sum1stColP = 0;
        for(int i = 0; i < m; i++) 
            sum1stColP += P[i][0];
        for(int i = 0; i < m; i++) 
            parameterWeights[i] = P[i][0]/sum1stColP;
        

        /***************************************************************************************************************
         *Second step: Option Weight Matrices respect parameter k, O[n][n][m]
         ***************************************************************************************************************/
        
        // sd is standard deviation of raw score respect each parameter
        double[] sd = new double[m];
        double[] ave = new double[m]; // the average
        for(int i = 0; i < m; i ++) {
            for(int j = 0; j < n; j++) 
                ave[i] += R[i][j];
            ave[i] = ave[i]/n;
        }
        // sum of square of elements minuse average
        for(int i = 0; i < m; i++) {
            for(int j = 0; j < n; j++)
                sd[i] += Math.pow((R[i][j]-ave[i]),2);
            // Square root the sum over number of options
            sd[i] = Math.sqrt(sd[i]/n);
            // in case standard deviation is 0, assign it to 1
            if(sd[i] == 0)
                sd[i] = 1;
        }
          
        // b.	Calculate the options matrix O respect parameter k        
        for(int k=0; k<m; k++) {
            for(int i=0; i<n; i++) {
                for(int j=0; j<n; j++) {
                    if("high".equals(param[k][2])) {
                        if(R[k][i] >= R[k][j])
                            O[i][j][k] = 1 + ((2*(R[k][i]-R[k][j]))/sd[k]);
                        else
                            O[i][j][k] = 1/(1 + ((2*(R[k][j]-R[k][i]))/sd[k]));
                    }
                    else {
                        if(R[k][i] <= R[k][j])
                            O[i][j][k] = 1 + ((2*(R[k][j]-R[k][i]))/sd[k]);
                        else
                            O[i][j][k] = 1/(1 + ((2*(R[k][i]-R[k][j]))/sd[k]));
                    }
                }
            }
        }
        
        
        /***************************************************************************************************************
         *Third step: Utility Score, S[n]
         ***************************************************************************************************************/
        
        // a. Normalize elements in O dividing by the summation of column respect each parameter k
        double[] sum1stColO = new double[m];
        for(int j = 0; j < m; j++) 
            for(int i = 0; i < n; i++) 
                sum1stColO[j] += O[i][0][j];
        
        double[] max = new double [m];
        for(int j = 0; j < m; j++) {
            for(int i = 0; i < n; i++) {
                optionWeights[i][j] = O[i][0][j] / sum1stColO[j];
                if (optionWeights[i][j] > max[j])
                    max[j] = optionWeights[i][j];
            }
        }
        
        // b. Divided  each elements in column by the maximum elements in that column get optionWeight[n][m]      
        for(int j = 0; j < m; j++) 
            for(int i = 0; i < n; i++)
                optionWeights[i][j] /= max[j];
  
        // c.	Multiply optionWeight by parameterWeight to get utility score S, and then 
        double sumS=0;
        for(int i=0; i<n; i++) {
            for(int j=0; j<m; j++) 
                S[i] += optionWeights[i][j]*parameterWeights[j];
            if(S[i]>S[optimalOption]) 
                    optimalOption = i;
            sumS += S[i];
        }
        
        // d. Normalize U by diving the sum of the elements. 
        //    The first entry will correspond to the first option and will continue in the order of options.
        for(int i=0; i<n; i++)
            S[i] /= sumS;
        
        
        /***************************************************************************************************************
         *Forth step: Confidence Scores, SDR[m][n-1]
         ***************************************************************************************************************/
        
        //a. Normalize elements in O dividing by the summation of the optimal option column (z) respect each parameter k
        int z = optimalOption; // optimal option column is z
        double sumSDR=0;
        double E=0; // the change in raw score in terms of standard deviations
        double[] sumC = new double[m]; // summation of the optimal option column (z)
        for(int j = 0; j < m; j++) 
            for(int i = 0; i < n; i++) 
                sumC[j] += O[i][z][j];
        
        //b. After changing in the Option Weight Matrices, 
        //   E, the new utility score of option will pass the new utility score of optimal option. 
        //   How many stander deviation, SDR[m][n-1], change in the raw data depends on the range of original raw date
        
        for(int i = 0; i < m; i++) {
            boolean moveColOneLeft=false;
            for(int j = 0; j < n; j++) {
                if(j == z)
                    moveColOneLeft=true;
                else {
                    E = (sumC[i]*sumC[i]*(S[z]-S[j]))/((1-O[j][z][i]+sumC[i])*parameterWeights[i]-sumC[i]*(S[z]-S[j]));
                    if(O[j][z][i] >= 1) {
                        if(moveColOneLeft)
                            SDR[i][j-1] = E;
                        else
                            SDR[i][j] = E;
                    }
                    else {
                        if (O[j][z][i] + E <= 1) {
                            if(moveColOneLeft)
                                SDR[i][j-1] = (1/O[j][z][i] - 1/(O[j][z][i]+E))/2;
                            else 
                                SDR[i][j] = (1/O[j][z][i] - 1/(O[j][z][i]+E))/2;
                        }
                        else {
                            if(moveColOneLeft)
                                SDR[i][j-1] = (1/O[j][z][i]+O[j][z][i]+E-2)/2;
                            else
                                SDR[i][j] = (1/O[j][z][i]+O[j][z][i]+E-2)/2;
                        }
                    }
                    if(moveColOneLeft)
                        sumSDR += SDR[i][j-1];
                    else
                        sumSDR += SDR[i][j];
                }
            }
        } 
        aveSDR = sumSDR/(m*(n-1));
        optimalOption += 1;
        
        
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
                    
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Trade Study Result</title>");
            out.println("<link href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\" rel=\"stylesheet\">");
            out.println("<link rel=\"stylesheet\" href=\"css/style.css\">");
            out.println("</head>");
            out.println("<body><div id='saveExcel'>");
            out.println("<h1>Information</h1>");
            out.println("<h2>Trade Study Name: " + info[0] + "</h2>");
            out.println("<h2>Program Name: " + info[1] + "</h2>");
            out.println("<h2>Topic Name: " + info[2] + "</h2>");
            out.println("<h2>Author: " + info[3] + "</h2>");
            out.println("");
            out.println("<h1>The Optimal Option is: Option " + optimalOption + "</h1>");
            out.println("");
            out.println("<h1>Utility Scores: </h1>");
            for(int i=0; i<S.length; i++)
                out.println("<h2>Option " + (i+1) + ": " + S[i] +"</h2>");
            out.println("");
            out.println("<h1>Option Sensitivity: " + aveSDR + "</h1>");
            out.println("");
            out.println("<h1>Option Sensitivity Matrix: </h1>");
            for(int  i=0; i<SDR.length; i++) {
                for(int j=0; j<SDR[0].length; j++) {
                    out.println("<h2>" + SDR[i][j] + " </h2>");
                }
                out.println("");
            }
            out.println("");
            out.println("");
            out.println("<h1>Parameter Weights: </h1>");
            for(int i=0; i<parameterWeights.length; i++)
                out.println("<h2>Parameter " + (i+1) + ": " + parameterWeights[i] +"</h2>");
            out.println("<table id='tsInfo' style='display:none'><tr><td>Trade Study Name: </td><td>"+info[0]+"</td></tr><tr><td>Program Name: </td><td>"+info[1]+"</td></tr><tr><td>Topic Name: </td><td>"+info[2]+"</td></tr><tr><td>Author Name: </td><td>"+info[3]+"</td></tr></table>");
            
            String paramTable = "";
            paramTable += "<tr><td>Name</td><td>Type</td><td>Preference</td><td>Comparesion</td></tr>";
            for(int i=0; i<m; i++) {
                paramTable += "<tr><td>" + param[i][0] + "</td>" + "<td>" + param[i][1] + "</td>" + "<td>" + param[i][2] + "</td>" + "<td>" + param[i][3] + "</td></tr>";
            }
            out.println("<table id='paramInfo' style='display:none'>" + paramTable + "</table>");
            
            String optTable = "";
            optTable += "<tr><td>Name</td></tr>";
            for(int i=0; i<n; i++) {
                optTable += "<tr><td>" + optNames[i] + "</td></tr>";
            }
            out.println("<table id='optInfo' style='display:none'>" + optTable + "</table>");
            
            String rawTable = "";
            rawTable += "<tr><td></td>";
            for(int i=0; i<n; i++) {
                rawTable += "<td>Option " + (i+1) + "</td>";
            }
            rawTable += "</tr>";
            for(int i=0; i<m; i++) {
                rawTable += "<tr><td>Parameter " + (i+1) + "</td>";
                for(int j=0; j<n; j++) {
                    rawTable += "<td>" + R[i][j] + "</td>";
                }
                rawTable += "</tr>";
            }
            out.println("<table id='rawInfo' style='display:none'>" + rawTable + "</table>");

            out.println("<iframe id='txtArea1' style='display:none'></iframe>");
            out.println("<input type='button' name='export' class='action-button' onclick='fnExcelReport()' value='Save Excel' />");
            out.println("<div class='dme_link'><p><a href='index.html'>Home Page</a></p></div>");
            out.println("<script src='js/dynamicForm.js'></script>");
            out.println("</div></body>");
            out.println("</html>");
            
        }
    }
}


