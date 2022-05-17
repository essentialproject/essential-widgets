/**
 * Copyright (c)2013 Enterprise Architecture Solutions Ltd.
 * This file is part of Essential Architecture Manager, 
 * the Essential Architecture Meta Model and The Essential Project.
 *
 * Essential Architecture Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Essential Architecture Manager is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Essential Architecture Manager.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * 23.05.2013	JWC	1st coding.
 */
package com.enterprise_architecture.essential.widgets;

/**
 * Interface that all clients of the HttpReportServiceClient and ReportServiceClient should implement 
 * to receive progress updates
 * @author Jonathan Carter
 * @version 1.0 - 23.05.2013
 * @see com.enterprise_architecture.essential.widgets.HttpReportServiceClient HttpReportServiceClient
 * @see com.enterprise_architecture.essential.widgets.ReportServiceClient ReportServiceClient
 *
 */
public interface ProgressListener 
{
	/**
	 * Receive a progress update from the ReportServiceClient / HttpReportServiceClient
	 * @param theMessage current output from the execution of the report service client. The report service
	 * clients will call this method at least once (on completing all the tasks) with the output messages
	 * that were generated from the execution of the process of sending the repository snapshot.
	 * @param theProgressPercentage the percentage of the overall task that has been
	 * completed.
	 */
	public void updateProgress(String theMessage, int theProgressPercentage);

}
