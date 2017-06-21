package fr.sparna.validator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.mminf.qskos4j.issues.Issue;
import at.ac.univie.mminf.qskos4j.issues.Issue.IssueType;
import at.ac.univie.mminf.qskos4j.issues.labels.util.EmptyLabelsResult;
import at.ac.univie.mminf.qskos4j.issues.labels.util.LabelConflict;
import at.ac.univie.mminf.qskos4j.issues.labels.util.LabelConflictsResult;
import at.ac.univie.mminf.qskos4j.issues.language.IncompleteLangCovResult;
import at.ac.univie.mminf.qskos4j.issues.language.OmittedOrInvalidLanguageTagsResult;
import at.ac.univie.mminf.qskos4j.issues.language.util.NoCommonLanguagesResult;
import at.ac.univie.mminf.qskos4j.issues.relations.UnidirectionallyRelatedConceptsResult;
import at.ac.univie.mminf.qskos4j.result.CollectionResult;
import at.ac.univie.mminf.qskos4j.result.Result.ReportFormat;

public class Process {
	private final Logger logger = LoggerFactory.getLogger(Process.class);
	
	protected List<SkosError> resultList = new ArrayList<SkosError>();
	// language used to generate the report
	protected String lang;
	
	protected Integer rulesFail;
	
	public Process(String lang) {
		super();
		this.lang = lang;
	}

	
	public Integer getRulesFail() {
		return rulesFail;
	}


	@SuppressWarnings("rawtypes")
	public List<SkosError> createReport(Collection<Issue> issues) throws IOException, OpenRDFException {

		int occurrenceCountFail = 0;
		BufferedWriter bw = null;
		FileWriter fw = null;
		fw = new FileWriter("report.txt");
		bw = new BufferedWriter(fw);
		// for each issue...
		for (Issue issue : issues) {
			
			
			if(issue.getType()==IssueType.ANALYTICAL){
				
				
				bw.write(issue.getResult().getData().toString());
				
				SkosError error=new SkosError();
				error.setId(issue.getId());
				
				String stateText = "";
				if (issue.getResult().isProblematic()) {
					stateText = "FAIL";
					error.setSuccess(false);
					occurrenceCountFail++;
					String occurrenceCount = Long.toString(issue.getResult().occurrenceCount());
					error.setNumber(occurrenceCount);
					stateText += " (" +occurrenceCount+ ")";
				} else {
					stateText = "OK";
					error.setSuccess(true);
				}
				logger.info("********************"+issue.getName()+" "+stateText+"************************\n\n");
				logger.info("********************Additional information************************");
				logger.info(issue.getResult().getData()+"\n\n");
				logger.info("******************************************************************");
				error.setState(stateText);
				
				// find issue 's description in user language
				IssueDescription desc = ValidatorConfig.getInstance().getApplicationData().findIssueDescriptionById(issue.getId());
				error.setDescription(desc.getDescriptionByLang(lang));
				error.setRuleName(desc.getLabelByLang(lang));

				// TODO : store also in IssueDescription
				if(issue.getWeblink()==null){
					error.setWeblink("");
				}else{
					error.setWeblink(issue.getWeblink().stringValue());
				}
				
				System.out.println(issue.getId()+" : "+issue.getResult().getClass().getName());
				
				// store messages only if problematic
				// some Issues like "Common Languages" return the correct languages even if there is no issue, and we don't want to display it in this case
				
				if(issue.getResult().isProblematic()) {
					List<String> messages=new ArrayList<String>();
					
					/*-----------Incomplete language coverage----------*/
					if(issue.getResult() instanceof IncompleteLangCovResult){
						IncompleteLangCovResult res=(IncompleteLangCovResult) issue.getResult();
						res.getData().entrySet().stream().forEach(entry->{
							StringBuffer buffer = new StringBuffer();
							buffer.append("<a href=\""+entry.getKey().toString()+"\" target=\"_blank\">"+entry.getKey().toString()+"</a> : ");
							entry.getValue().stream().forEach(string -> buffer.append(string+ " "));
							buffer.append("\n");
							messages.add(buffer.toString());
						});
					}
					/*-----------------Unidirectional related concept----*/
					else if(issue.getResult() instanceof UnidirectionallyRelatedConceptsResult){
						UnidirectionallyRelatedConceptsResult res=(UnidirectionallyRelatedConceptsResult) issue.getResult();
						res.getData().entrySet().stream().forEach(entry->{
							StringBuffer buffer = new StringBuffer();
							entry.getKey().getElements().stream().forEach(aResource -> {
								buffer.append("<a href=\""+aResource.toString()+"\" target=\"_blank\">"+aResource.toString()+"</a>");
								buffer.append(", ");
							});
							buffer.delete(buffer.length()-2, buffer.length());
							buffer.append(" : ");
							buffer.append(entry.getValue());
							buffer.append("\n");
							messages.add(buffer.toString());
						});
					}
					/*-------------------Omitted or Invalid Language Tags------------*/
					else if(issue.getResult() instanceof OmittedOrInvalidLanguageTagsResult){
						OmittedOrInvalidLanguageTagsResult res=(OmittedOrInvalidLanguageTagsResult) issue.getResult();
						res.getData().entrySet().stream().forEach(entry->{
							StringBuffer buffer = new StringBuffer();
							buffer.append("<a href=\""+entry.getKey().toString()+"\" target=\"_blank\">"+entry.getKey().toString()+"</a> : ");
							entry.getValue().stream().forEach(aLiteral -> {
								buffer.append(aLiteral.toString()+", ");
							});
							buffer.delete(buffer.length()-2, buffer.length());
							messages.add(buffer.toString());
						});
					}
					/*-------------------Empty labels--------------------------------*/
					else if(issue.getResult() instanceof EmptyLabelsResult){
						EmptyLabelsResult res=(EmptyLabelsResult) issue.getResult();
						res.getData().entrySet().stream().forEach(entry->{
							StringBuffer buffer = new StringBuffer();
							buffer.append("<a href=\""+entry.getKey().toString()+"\" target=\"_blank\">"+entry.getKey().toString()+"</a> : ");
							entry.getValue().stream().forEach(aLabelType -> {
								buffer.append(aLabelType.toString()+", ");
							});
							buffer.delete(buffer.length()-2, buffer.length());
							messages.add(buffer.toString());						
						});
					}
					else if(issue.getResult() instanceof NoCommonLanguagesResult) {
						NoCommonLanguagesResult res=(NoCommonLanguagesResult) issue.getResult();
						res.getData().stream().forEach(aString->{
							messages.add(aString);
						});
					}
					/*--------------------Collection Result-------------------------*/
					else if(issue.getResult() instanceof CollectionResult<?>) {
						CollectionResult collectionResult = (CollectionResult)issue.getResult();
						Collection<?> collection = (Collection<?>)collectionResult.getData();
						collection.forEach(item-> {
							System.out.println("  "+issue.getId()+" : "+item.getClass().getName());
							if(item instanceof URI) {
								messages.add("<a href=\""+item.toString()+"\" target=\"_blank\">"+item.toString()+"</a>");
							} else if(item instanceof LabelConflict) {
								messages.add(item.toString());		
								// TODO : improve class LabelConflict to get our hand on the inner LabeledResource to get the label
	//							LabelConflict aConflict = (LabelConflict)item;
	//							StringBuffer buffer = new StringBuffer();	//							
	//							buffer.append("<ul>\n");
	//							aConflict.getAffectedResources().stream().forEach(anAffectedResource-> {
	//								buffer.append("<li>"+anAffectedResource.toString()+"</li>");
	//							});
	//							buffer.append("</ul>\n");
	//							messages.add(buffer.toString());
							} else if (item instanceof Collection) {
								// collections inside a collection, case of "disconnected concept clusters"
								StringBuffer buffer = new StringBuffer();
								Collection c = (Collection)item;								
								c.stream().forEach(elmt -> {
									if(elmt instanceof URI) {
										buffer.append("<a href=\""+elmt.toString()+"\" target=\"_blank\">"+elmt.toString()+"</a>");
									} else {
										buffer.append(elmt.toString());
									}
									buffer.append(", ");
								});
								buffer.delete(buffer.length()-2, buffer.length());
								messages.add(buffer.toString());
							} else {
								messages.add(item.toString());
							}
						}					
						);
					} else {
						System.out.println("Unknown result type "+issue.getResult().getClass().getName());
					}
					
					error.setErrorList(messages);
				}
				resultList.add(error);
			}
		}
		bw.close();
		fw.close();
		this.rulesFail=occurrenceCountFail;
		return resultList;
	}
}