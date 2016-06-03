/*******************************************************************************
 * This file is part of ISPyB.
 * 
 * ISPyB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * ISPyB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with ISPyB.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors : S. Delageniere, R. Leal, L. Launer, K. Levik, S. Veyrier, P. Brenchereau, M. Bodin, A. De Maria Antolinos
 ******************************************************************************************************************************/

package ispyb.server.mx.services.ws.rest.DataCollection;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.transform.AliasToEntityMapResultTransformer;


@Stateless
public class DataCollectionRestWsServiceBean implements DataCollectionRestWsService, DataCollectionRestWsServiceLocal {
	/** The entity manager. */
	@PersistenceContext(unitName = "ispyb_db")
	private EntityManager entityManager;


	private String getPhasingSpaceGroupQuery(){
		return "  (SELECT GROUP_CONCAT(distinct(`pydb`.`SpaceGroup`.`spaceGroupShortName`)) AS `v_datacollection_summary_phasing_spaceGroupShortName`\n" + 
				"    FROM `pydb`.`AutoProcIntegration` " + 
				"        LEFT JOIN `pydb`.`AutoProcScaling_has_Int` ON `pydb`.`AutoProcScaling_has_Int`.`autoProcIntegrationId` = `pydb`.`AutoProcIntegration`.`autoProcIntegrationId` " + 
				"        LEFT JOIN `pydb`.`AutoProcScaling` ON `pydb`.`AutoProcScaling`.`autoProcScalingId` = `pydb`.`AutoProcScaling_has_Int`.`autoProcScalingId` " + 
				"        LEFT JOIN `pydb`.`Phasing_has_Scaling` ON `pydb`.`Phasing_has_Scaling`.`autoProcScalingId` = `pydb`.`AutoProcScaling`.`autoProcScalingId` " + 
				"        LEFT JOIN `pydb`.`PhasingStep` ON `pydb`.`PhasingStep`.`autoProcScalingId` = `pydb`.`Phasing_has_Scaling`.`autoProcScalingId` " + 
				"        LEFT JOIN `pydb`.`SpaceGroup` ON `pydb`.`SpaceGroup`.`spaceGroupId` = `pydb`.`PhasingStep`.`spaceGroupId` " +
				"		where `pydb`.`AutoProcIntegration`.`dataCollectionId` = v_datacollection_summary.DataCollection_dataCollectionId " +  
				") as Phasing_spaceGroup, ";
		
	}
	
	private String getPhasingStepQuery(){
		return "  (SELECT GROUP_CONCAT(distinct(`pydb`.`PhasingStep`.`phasingStepType`)) AS `v_datacollection_summary_phasing_spaceGroupShortName`\n" + 
				"    FROM `pydb`.`AutoProcIntegration` " + 
				"        LEFT JOIN `pydb`.`AutoProcScaling_has_Int` ON `pydb`.`AutoProcScaling_has_Int`.`autoProcIntegrationId` = `pydb`.`AutoProcIntegration`.`autoProcIntegrationId` " + 
				"        LEFT JOIN `pydb`.`AutoProcScaling` ON `pydb`.`AutoProcScaling`.`autoProcScalingId` = `pydb`.`AutoProcScaling_has_Int`.`autoProcScalingId` " + 
				"        LEFT JOIN `pydb`.`Phasing_has_Scaling` ON `pydb`.`Phasing_has_Scaling`.`autoProcScalingId` = `pydb`.`AutoProcScaling`.`autoProcScalingId` " + 
				"        LEFT JOIN `pydb`.`PhasingStep` ON `pydb`.`PhasingStep`.`autoProcScalingId` = `pydb`.`Phasing_has_Scaling`.`autoProcScalingId` " + 
				"        LEFT JOIN `pydb`.`SpaceGroup` ON `pydb`.`SpaceGroup`.`spaceGroupId` = `pydb`.`PhasingStep`.`spaceGroupId` " +
				"		where `pydb`.`AutoProcIntegration`.`dataCollectionId` = v_datacollection_summary.DataCollection_dataCollectionId " +  
				") as Phasing_phasingStepType, ";
		
	}
	
	private String getViewTableQuery(){
		return "select *, "
				+ "(select GROUP_CONCAT(workflowStepId) from WorkflowStep \n" + 
				" where WorkflowStep.workflowId = v_datacollection_summary.Workflow_workflowId order by  WorkflowStep.workflowStepId DESC) as WorkflowStep_workflowStepId,"
				+ "(select GROUP_CONCAT(workflowStepType) from WorkflowStep where WorkflowStep.workflowId = v_datacollection_summary.Workflow_workflowId) as WorkflowStep_workflowStepType,"  
				+ "(select GROUP_CONCAT(status) from WorkflowStep where WorkflowStep.workflowId = v_datacollection_summary.Workflow_workflowId) as WorkflowStep_status,"
				+ " GROUP_CONCAT(`AutoProcProgram_processingPrograms` SEPARATOR ', ') AS `processingPrograms`, "
				+ " GROUP_CONCAT(`AutoProcProgram_processingStatus` SEPARATOR ', ') AS `processingStatus`,"
				+ " GROUP_CONCAT(`AutoProcIntegration_autoProcIntegrationId` SEPARATOR ', ') AS `autoProcIntegrationId`, "
				+ this.getPhasingSpaceGroupQuery()
				+ this.getPhasingStepQuery()
				+ " (select SUM(numberOfImages) FROM DataCollection where dataCollectionGroupId = v_datacollection_summary.DataCollectionGroup_dataCollectionGroupId) as totalNumberOfImages,"
				+ " (select MAX(imageId) FROM Image where dataCollectionId = v_datacollection_summary.DataCollection_dataCollectionId) as lastImageId,"
				+ " (select MAX(imageId) FROM Image where dataCollectionId = v_datacollection_summary.DataCollection_dataCollectionId) as firstImageId"
				+ " from v_datacollection_summary";
	}
	
	
	@Override
	public List<Map<String, Object>> getViewDataCollectionBySessionId(int proposalId, int sessionId) {
		String mySQLQuery = getViewTableQuery() + " where DataCollectionGroup_sessionId = :sessionId and BLSession_proposalId = :proposalId ";
		mySQLQuery = mySQLQuery + " group by v_datacollection_summary.DataCollectionGroup_dataCollectionGroupId, v_datacollection_summary.DataCollectionGroup_dataCollectionGroupId";
		Session session = (Session) this.entityManager.getDelegate();
		SQLQuery query = session.createSQLQuery(mySQLQuery);
		query.setParameter("sessionId", sessionId);
		query.setParameter("proposalId", proposalId);
		query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> aliasToValueMapList = query.list();
		return aliasToValueMapList;
	}
	
	@Override
	public List<Map<String, Object>> getViewDataCollectionByProteinAcronym(int proposalId, String proteinAcronym) {
		String mySQLQuery = getViewTableQuery() + " where BLSession_proposalId = :proposalId and Protein_acronym = :proteinAcronym";
		mySQLQuery = mySQLQuery + " group by v_datacollection_summary.DataCollectionGroup_dataCollectionGroupId, v_datacollection_summary.DataCollectionGroup_dataCollectionGroupId";
		Session session = (Session) this.entityManager.getDelegate();
		SQLQuery query = session.createSQLQuery(mySQLQuery);
		query.setParameter("proposalId", proposalId);
		query.setParameter("proteinAcronym", proteinAcronym);
		query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> aliasToValueMapList = query.list();
		return aliasToValueMapList;
	}

	@Override
	public Collection<? extends Map<String, Object>> getViewDataCollectionByDataCollectionId(int proposalId, int dataCollectionId) {
		String mySQLQuery = getViewTableQuery() + " where DataCollection_dataCollectionId = :dataCollectionId and BLSession_proposalId = :proposalId ";
		mySQLQuery = mySQLQuery + " group by v_datacollection_summary.DataCollectionGroup_dataCollectionGroupId, v_datacollection_summary.DataCollectionGroup_dataCollectionGroupId";
		Session session = (Session) this.entityManager.getDelegate();
		SQLQuery query = session.createSQLQuery(mySQLQuery);
		query.setParameter("dataCollectionId", dataCollectionId);
		query.setParameter("proposalId", proposalId);
		query.setResultTransformer(AliasToEntityMapResultTransformer.INSTANCE);
		@SuppressWarnings("unchecked")
		List<Map<String, Object>> aliasToValueMapList = query.list();
		return aliasToValueMapList;
	}

	
}