<%--
  The contents of this file are subject to the OpenMRS Public License
  Version 1.0 (the "License"); you may not use this file except in
  compliance with the License. You may obtain a copy of the License at
  http://license.openmrs.org

  Software distributed under the License is distributed on an "AS IS"
  basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
  License for the specific language governing rights and limitations
  under the License.

  Copyright (C) OpenMRS, LLC.  All Rights Reserved.

--%>
<%@ include file="/WEB-INF/template/include.jsp" %>
<openmrs:require privilege="Import Spreadsheet Import Templates" otherwise="/login.htm" redirect="/module/spreadsheetimport/spreadsheetimportImport.form"/>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>
<%@ page session="true" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/html; charset=UTF-8" %>

<style>
    table {
        border-collapse: collapse;
        width: 100%;
    }

    tr:nth-child(even) {background-color: #f2f2f2;}

    th, td {
        padding: 12px;
        text-align: left;
    }

    #progressBar {
        width: 2%;
        height: 30px;
        background-color: #4CAF50;
        text-align: center; /* To center it horizontally (if you want) */
        line-height: 30px; /* To center it vertically */
        color: white;
    }

    #completionStatus {
        font-size: 16px;
    }

    .currentlyProcessing {
        color: #4CAF50 ;
    }

</style>

<script type="text/javascript">

    $j(document).ready(function () {

        $j("#migrateAll").click(function(event){
            event.preventDefault();
            $j(this).attr('disabled', true);
            processAllDatasets();
        });

        setInterval(getMigrationDatasetUpdates, 10000);

    });


    function processAllDatasets() {
        DWRMigrationService.processAllDatasets(function(completionMsg){
            updateProgressBar(0, 0);
            $j("#completionStatus").html(completionMsg);


        });
    }

    function getMigrationDatasetUpdates() {
        DWRMigrationService.getMigrationDatasetUpdates(function(mapResult){
            $j("#migrationUpdates").find("tbody").empty();
            var tableRef = document.getElementById("migrationUpdates").getElementsByTagName('tbody')[0];
            var countOfAllRows = 0;
            var countOfProcessedRows = 0;
            for (var key in mapResult) {

                var myHtmlContent = "<td>" + key + "</td><td>" + Number(mapResult[key].totalRowCount).toLocaleString('en-US') + "</td><td>" + Number(mapResult[key].processedCount).toLocaleString('en-US') + "</td>";
                var newRow = tableRef.insertRow(tableRef.rows.length);
                newRow.innerHTML = myHtmlContent;
                //set class if it's currently processing
                if (Number(mapResult[key].totalRowCount) > 0 && Number(mapResult[key].processedCount) > 0 && Number(mapResult[key].totalRowCount) != Number(mapResult[key].processedCount)) {
                    newRow.classList.add("currentlyProcessing");
                }
                countOfAllRows += Number(mapResult[key].totalRowCount);
                countOfProcessedRows += Number(mapResult[key].processedCount);
            }
            if (countOfProcessedRows > 0) { // only update this if processing has started
                updateProgressBar(countOfAllRows, countOfProcessedRows);
            }

        });
    }

    function updateProgressBar(totalRowCount, totalProcessed){
        var pBar = document.getElementById("progressBar");
        var width = 2;

        if(totalRowCount != 0 && totalProcessed != 0) {
            width = Math.floor((totalProcessed * 100)/totalRowCount);
        }

        pBar.style.width = width + "%";
        pBar.innerHTML = width + "%";

    }



</script>

<form method="post" enctype="multipart/form-data">
    <form:errors path="*" cssClass="error"/>

    <h3>Migrate Data</h3>
    <br/>
    <button id="migrateAll">Migrate all Datasets</button>
    <br/>
    <br/>
    <div id="migrationProgress">
        <div id="progressBar">0%</div>
    </div>
    <br/>
    <div id="completionStatus"></div>

    <br/>


    <table id="migrationUpdates">
        <thead>
        <tr>
            <th>Dataset Name</th>
            <th>Total Records</th>
            <th>Total processed</th>
        </tr>
        </thead>
        <tbody>

        </tbody>
    </table>
</form>



<%@ include file="/WEB-INF/template/footer.jsp"%>
