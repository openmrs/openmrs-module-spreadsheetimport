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
<openmrs:require privilege="Save Spreadsheet Import Template" otherwise="/login.htm" redirect="/module/spreadsheetimport/spreadsheetimport.form"/>
<%@ include file="/WEB-INF/template/header.jsp" %>
<%@ include file="localHeader.jsp" %>
<%@ taglib prefix="formsim" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>

<b>Step 2 of 2: Pre-specified Values</b>
<p/>

<formsim:form method="post" id="form1" name="form1" commandName="template" htmlEscape="true" action="spreadsheetimport.form">
    <formsim:errors path="*" cssClass="error"/>
    <input type="hidden" name="step" value="prespecifiedValues"/>
   	<table cellpadding="2">
   		<tr>
   			<td>Name:</td>
   			<td>${template.name}</td>
		</tr>
		<tr>
			<td>Description:</td>    		
			<td>${template.description}</td>
		</tr>
		<c:if test="${template.encounter}">
			<tr>
				<td>Form:</td>    		
				<td>${template.targetForm}</td>
			</tr>
		</c:if>
		<tr>
			<td colspan="2">&nbsp;</td>
		</tr>
		<tr>
			<td colspan="2">
				<table>
					<tr>
						<th>Table</th>
						<th>Value</th>
						<th>Linked Columns</th>
					</tr>
					<c:forEach var="prespecifiedValue" items="${template.prespecifiedValues}" varStatus="status">
						<tr>
							<td>${prespecifiedValue.prettyTableName}</td>								
							<td><formsim:select path="prespecifiedValues[${status.index}].value">



								<formsim:option value="" label="${prespecifiedValues[status.index].value}"/>
								<formsim:options items="${prespecifiedValue.mapNameToAllowedValue}" itemLabel="name" itemValue="value"  />
							</formsim:select></td>
							<td><table>
								<tr>
									<th>Name</th>
									<th>Data</th>
								</tr>
								<c:forEach var="column" items="${prespecifiedValue.columns}">
									<tr>
										<td>${column.name}</td>
										<td>${column.data}</td>
									</tr>
								</c:forEach>
							</table></td>
						</tr>
					</c:forEach>
				</table>
			</td>
		</tr>
		<tr>
			<td colspan="2">&nbsp;</td>
		</tr>
		<tr>
   			<td><input type="submit" name="Previous Step" value="Previous Step"/>
   			<input type="submit" value="Save"/></td>
   		</tr>
   	</table>
</formsim:form>

<%@ include file="/WEB-INF/template/footer.jsp" %>