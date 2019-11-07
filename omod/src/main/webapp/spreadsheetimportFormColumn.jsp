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
<openmrs:require privilege="Save Spreadsheet Import Template" otherwise="/login.htm" redirect="spreadsheetimport.form"/>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>
<%@ page session="true" %>
<%@ page pageEncoding="UTF-8" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="formsim" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<b>Step 1 of 2: Columns</b>
<p/>

<formsim:form method="post" id="form1" name="form1" commandName="template" htmlEscape="true" action="spreadsheetimport.form">
    <formsim:errors path="*" cssClass="error"/>
    <input type="hidden" name="step" value="columns"/>
   	<table cellpadding="2">
   		<tr>
   			<td>Name:</td>
   			<td><formsim:input path="name" id="name" autocomplete="off" size="25" /><div class="fieldError"><formsim:errors path="name" /></div></td></td>
		</tr>
		<tr>
			<td>Description:</td>    		
			<td><formsim:input path="description" id="description" size="70" /></td>
		</tr>		
		<tr>
			<td>Create an encounter for every imported line:</td>    		
			<td><formsim:checkbox path="encounter" id="encounter" onclick="check(this.form)" /></td>
		</tr>
		<tr>
			<td>Form to show data:</td>    		
			<td><formsim:select path="targetForm" id="targetForm" disabled="true" >
				<c:forEach var="dataform" items="${dataforms}">
					<formsim:option value="${dataform.id}">${dataform.name}</formsim:option>
				</c:forEach>
			</formsim:select></td>
		</tr>
		<c:if test="${fn:length(template.columns) > 0}">
			<tr>
				<td colspan="2">&nbsp;</td>
			</tr>
			<tr>
				<td colspan="2">
					<table>
						<tr>
							<th>Name</th>
							<th>Data</th>
							<th>Disallow<br/>Duplicate<br/>Value</th>
							<th>Dataset Index<br/>(Optional)</th>							
							<th>Delete?</th>
						</tr>
						<c:forEach var="column" items="${template.columns}" varStatus="status">
							<tr>
								<td><formsim:input path="columns[${status.index}].name"/></td>								
								<td><formsim:select path="columns[${status.index}].tableDotColumn">
									<formsim:option value="" label="Select"/>
									<formsim:options items="${tableColumnMap}"/>
								</formsim:select></td>
								<td><formsim:checkbox path="columns[${status.index}].disallowDuplicateValue"/></td>
								<td><formsim:select path="columns[${status.index}].datasetIdx">
									<formsim:option value="" label="Unique Import into Table"/>
									<%
										for (int i=0; i<200; i++) {
									%>
									<formsim:option value="<%= i%>" label="<%= Integer.toString(i+1)%>"/>
									<%
										}
									%>
								</formsim:select></td>
								<td><input type="checkbox" name="${status.index}"/></td>
							</tr>
						</c:forEach>
					</table>
				</td>
			</tr>
			<tr>
				<td colspan="2">&nbsp;</td>
			</tr>
		</c:if>
		<tr>
   			<td><input type="submit" name="Add Column" value="Add Column"/>
   			<input type="submit" name="Delete Columns" value="Delete Selected Columns"/>
   			<input type="submit" value="Next Step"/>
   			<input type="submit" id = "createFromForm" name="createFromForm" value="Create Template from Form"/>
   			</td>
   		</tr>
   	</table>
</formsim:form>

<SCRIPT TYPE="text/javascript">
<!--
function check(form)
{
	if(form.encounter.checked) {
		form.targetForm.disabled = false;
		form.createFromForm.disabled = false;
	} else {
		form.targetForm.disabled = true;
		form.createFromForm.disabled = true;
	}
}
//-->
</SCRIPT>

<%@ include file="/WEB-INF/template/footer.jsp" %>