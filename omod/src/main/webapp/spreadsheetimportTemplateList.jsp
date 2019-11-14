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
<openmrs:require privilege="List Spreadsheet Import Templates" otherwise="/login.htm" redirect="/module/spreadsheetimport/spreadsheetimport.list"/>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>

<h2>Spreadsheet Import Template List</h2>

<a href="spreadsheetimport.form">New</a>
<p />

<c:if test="${fn:length(templates) > 0}">
	<form method="post">
	   	<table cellpadding="2">
        	<tr>
        		<th>&nbsp;</th>
            	<th>Name</th>
            	<th>Description</th>
            	<th>Action</th>
        	</tr>
        	<c:forEach var="template" items="${templates}">
        	   	<tr>
               		<td><input type="checkbox" name="${template.id}"/></td>
               		<td><a href="spreadsheetimport.form?id=${template.id}">${template.name}</a></td>
                   	<td>${template.description}</td>
                   	<td><a href="spreadsheetimportImport.form?id=${template.id}">Import</a></td>
               	</tr>
        	</c:forEach>          
    	</table>
    	<p />
		<td><input type="submit" value="Delete"/></td>
		<td><input type="button" value="Migrate Datasets" onclick="window.location.href = 'spreadsheetimportprocessdatasets.form';"/></td>
	</form>
</c:if>

<%@ include file="/WEB-INF/template/footer.jsp" %>