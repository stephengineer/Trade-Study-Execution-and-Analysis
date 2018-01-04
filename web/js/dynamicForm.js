// hide nav bar and display new trade study
function newTrade() {
   window.location = "newTrade.html";
}

function loadTrade() {
   window.location = "loadTrade.html";
}

function fnExcelReport()
{
    var tab_text="<table border='2px'><tr bgcolor='#87AFC6'>";
    var textRange; var i=0;
    
    tab = document.getElementById('tsInfo'); // id of table
    for(i = 0 ; i < tab.rows.length ; i++)      
        tab_text=tab_text+tab.rows[i].innerHTML+"</tr>";
        
    tab = document.getElementById('paramInfo'); // id of table
    for(i = 0 ; i < tab.rows.length ; i++)      
        tab_text=tab_text+tab.rows[i].innerHTML+"</tr>";
    
    tab = document.getElementById('optInfo'); // id of table
    for(i = 0 ; i < tab.rows.length ; i++)      
        tab_text=tab_text+tab.rows[i].innerHTML+"</tr>";
    
    tab = document.getElementById('rawInfo'); // id of table
    for(i = 0 ; i < tab.rows.length ; i++)      
        tab_text=tab_text+tab.rows[i].innerHTML+"</tr>";

    tab_text = tab_text + "</table>";
    tab_text = tab_text.replace(/<A[^>]*>|<\/A>/g, ""); //remove if u want links in your table
    tab_text = tab_text.replace(/<img[^>]*>/gi, ""); // remove if u want images in your table
    tab_text = tab_text.replace(/<input[^>]*>|<\/input>/gi, ""); // reomves input params


    var ua = window.navigator.userAgent;
    var msie = ua.indexOf("MSIE "); 

    if (msie > 0 || !!navigator.userAgent.match(/Trident.*rv\:11\./))      // If Internet Explorer
    {
        txtArea1.document.open("txt/html","replace");
        txtArea1.document.write(tab_text);
        txtArea1.document.close();
        txtArea1.focus(); 
        sa=txtArea1.document.execCommand("SaveAs",true,"Trade Study.xls");
    }  
    else                 //other browser not tested on IE 11
        sa = window.open('data:application/vnd.ms-excel,' + encodeURIComponent(tab_text));  
    
    return (sa);
}
   

// Add , Dlelete row dynamically

var numParam = 1;
var numOpt = 1;

$(document).ready(function(){

    // add parameter
    $("#add_param").click(function() {
        $('#param'+numParam).html(
                "<td>" + (numParam + 1) + "</td>" +
                
                "<td><input name='paramName" + numParam + "' type='text' placeholder='Name' class='form-control input-md'/></td>" +

                "<td>" +
                    "<select type='text' name='type" + numParam + "' class='form-control'>" + 
                        "<option name='dataQuality' value='dataQuality'>Data Quality</option>" +
                        "<option name='eventManagement' value='eventManagement'>Event Management</option>" +
                        "<option name='deployment' value='deployment'>Deployment</option>" +
                    "</select>" +
                "</td>" +
                
                "<td>" +
                    "<input type='radio' name='pref" + numParam + "' value='high' />high" +
                    "<input type='radio' name='pref" + numParam + "' value='low' />low" +
                "</td>"+
                
                "<td>" +
                    "<input type='radio' name='comp" + numParam + "' value='equally' />equally" +
                    "<input type='radio' name='comp" + numParam + "' value='moderately' />moderately" +
                    "<input type='radio' name='comp" + numParam + "' value='greatly' />greatly" +
                    "<input type='radio' name='comp" + numParam + "' value='extremely' />extremely" +
                "</td>"
                
        );
                
        $('#tab_params').append('<tr id="param'+(numParam+1)+'"></tr>');
        numParam++; 
        document.getElementById('numParams').value = numParam;
        });        
    // delete parameter
    $("#delete_param").click(function(){
    	if(numParam>1){
            $("#param"+(numParam-1)).html('');
                numParam--;
                document.getElementById('numParams').value = numParam;
	}
    });
    //add option
    $("#add_opt").click(function(){
        $('#opt'+numOpt).html("<td>"+ (numOpt+1) +"</td><td><input name='optName"+numOpt+"' type='text' placeholder='Name' class='form-control input-md'  /> </td>");
        $('#tab_opt').append('<tr id="opt'+(numOpt+1)+'"></tr>');
        numOpt++; 
        document.getElementById('numOpts').value = numOpt;
    }); 
    // delete option
    $("#delete_opt").click(function(){
    	if(numOpt>1){
            $("#opt"+(numOpt-1)).html('');
            numOpt--;
            document.getElementById('numOpts').value = numOpt;
	}
    });         
    
    // create raw score matrix     
    $("#rawScore_matrix").click(function(){
        
        for(var i = 0; i < numParam+1; i++) {
            // first line print header of parameter
            if (i==0) 
                $('#p'+i).html("<td> </td>");
            else
                $('#p'+i).html("<td>"+ 'Parameter '+ i +"</td>");
            for(var j=0; j < numOpt+1; j++) {
                // print out header of option
                if (i==0) {
                    $('#p'+i+'o'+j).html("<h5>Option " + j + "</h5>");    
                    $('#p'+i).append('<td id="p'+i+'o'+(j+1)+'"></td>');  
                }
                // print out matrix of raw score
                else {
                    $('#p'+i+'o'+j).html("<input required name='p"+i+"o"+j+"' type='number' step='any' class='form-control' />");    
                    $('#p'+i).append('<td id="p'+i+'o'+(j+1)+'"></td>');    
                }
            }
          $('#tab_raw').append('<tr id="p'+(i+1)+'"></tr>');       
        }
        
    });      
});





