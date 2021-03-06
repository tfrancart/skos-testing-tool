package fr.sparna.rdf.skos.testtool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.univie.mminf.qskos4j.issues.Issue;
import at.ac.univie.mminf.qskos4j.issues.labels.util.EmptyLabelsResult;
import at.ac.univie.mminf.qskos4j.issues.labels.util.LabelConflict;
import at.ac.univie.mminf.qskos4j.issues.language.IncompleteLangCovResult;
import at.ac.univie.mminf.qskos4j.issues.language.OmittedOrInvalidLanguageTagsResult;
import at.ac.univie.mminf.qskos4j.issues.language.util.NoCommonLanguagesResult;
import at.ac.univie.mminf.qskos4j.issues.relations.UnidirectionallyRelatedConceptsResult;
import at.ac.univie.mminf.qskos4j.result.CollectionResult;
import at.ac.univie.mminf.qskos4j.util.IssueDescriptor.IssueType;
import at.ac.univie.mminf.qskos4j.util.Pair;
import fr.sparna.rdf.skos.testtool.IssueDescription.IssueLevel;

public class IssueConverter {
	private final Logger logger = LoggerFactory.getLogger(IssueConverter.class);

	protected List<IssueResultDisplay> resultList = new ArrayList<IssueResultDisplay>();
	// language used to generate the report
	protected String lang;

	protected Integer rulesFail;

	protected Long allconcepts;

	protected Long allconceptscheme;

	protected Long allcollection;

	public IssueConverter(String lang) {
		super();
		this.lang = lang;
	}


	public Integer getRulesFail() {
		return rulesFail;
	}

	public Long getAllconcepts() {
		return allconcepts;
	}

	public Long getAllconceptscheme() {
		return allconceptscheme;
	}

	public Long getAllcollection() {
		return allcollection;
	}

	@SuppressWarnings({ "rawtypes", "deprecation" })
	public List<IssueResultDisplay> createReport(Collection<Issue> issues) throws IOException {

		int occurrenceCountFail = 0;
		// for each issue...
		for (Issue issue : issues) {


			if(issue.getIssueDescriptor().getType()==IssueType.ANALYTICAL){
				IssueDescription desc = TestToolConfig.getInstance().getApplicationData().findIssueDescriptionById(issue.getIssueDescriptor().getId());
				IssueResultDisplay error=new IssueResultDisplay();
				error.setId(issue.getIssueDescriptor().getId());

				String stateText = "";
				String occurrenceaccount = "";
				if (issue.getResult().isProblematic()) {
					stateText = "FAIL";
					if(desc.getLevel() == IssueLevel.WARNING) {
						stateText = "WARNING";
					}
					error.setSuccess(false);
					occurrenceCountFail++;
					try{
						occurrenceaccount = Long.toString(issue.getResult().occurrenceCount());
						stateText += " (" +occurrenceaccount+ ")";
					}catch (UnsupportedOperationException e) {
		                // ignore this
		            }
					error.setNumber(occurrenceaccount);
					
				} else {
					stateText = "OK";
					error.setSuccess(true);
				}
				error.setState(stateText);
				
				// find issue's description in user language
				
				error.setDescription(desc.getDescriptionByLang(lang));
				error.setRuleName(desc.getLabelByLang(lang));
				error.setLevel(desc.getLevel());

				// TODO : store also in IssueDescription
				if(issue.getIssueDescriptor().getWeblink()==null){
					error.setWeblink("");
				}else{
					error.setWeblink(issue.getIssueDescriptor().getWeblink().toString());
				}

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
							
							if(item instanceof IRI) {
								messages.add("<a href=\""+item.toString()+"\" target=\"_blank\">"+item.toString()+"</a>");
							} else if(item instanceof LabelConflict) {

								LabelConflict aConflict = (LabelConflict)item;
								StringBuffer buffer = new StringBuffer();
								// le literal dupliqué
								Literal l = aConflict.getConflicts().iterator().next().getLiteral();
								buffer.append(l+" : ");
								aConflict.getConflicts().stream().forEach(aLabeledConcept -> {
									buffer.append("<a href=\""+aLabeledConcept.getConcept().toString()+"\" target=\"_blank\">"+aLabeledConcept.getConcept().toString()+"</a>"+" ("+aLabeledConcept.getLabelType().getUsualPrefixedDisplayUri()+")"+", ");
								});
								buffer.delete(buffer.length()-2, buffer.length());
								messages.add(buffer.toString());
								
							} else if (item instanceof Collection) {
								// collections inside a collection, case of "disconnected concept clusters"
								StringBuffer buffer = new StringBuffer();
								buffer.append("Cluster : ");
								Collection c = (Collection)item;								
								c.stream().forEach(elmt -> {
									if(elmt instanceof IRI) {
										buffer.append("<a href=\""+elmt.toString()+"\" target=\"_blank\">"+elmt.toString()+"</a>");
									} else {
										buffer.append(elmt.toString());
									}
									buffer.append(", ");
								});
								buffer.delete(buffer.length()-2, buffer.length());
								messages.add(buffer.toString());
							} else if (item instanceof Pair) {
								// hr hierarchical redundancy
								Pair p = (Pair)item;
								if(p.getFirst() instanceof IRI) {
									StringBuffer buffer = new StringBuffer();
									buffer.append("[ ");
									buffer.append("<a href=\""+p.getFirst().toString()+"\" target=\"_blank\">"+p.getFirst().toString()+"</a>");
									buffer.append(" - ");
									buffer.append("<a href=\""+p.getSecond().toString()+"\" target=\"_blank\">"+p.getSecond().toString()+"</a>");
									buffer.append(" ]");
									messages.add(buffer.toString());
								} else {
									messages.add(p.toString());
								}
							} else if (item instanceof Statement) {
								// mrl - mapping relation misuse
								Statement s = (Statement)item;
								StringBuffer buffer = new StringBuffer();
								buffer.append("<");
								buffer.append("<a href=\""+s.getSubject()+"\" target=\"_blank\">"+s.getSubject()+"</a>");
								buffer.append("> <");
								buffer.append("<a href=\""+s.getPredicate()+"\" target=\"_blank\">"+s.getPredicate()+"</a>");
								buffer.append("> <");
								buffer.append("<a href=\""+s.getObject()+"\" target=\"_blank\">"+s.getObject()+"</a>");
								buffer.append(">");
								messages.add(buffer.toString());
							}
							 else {
								System.out.println(item.getClass().getName());
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
			}else{
				//allcollection
				if(issue.getIssueDescriptor().getId().equals("cc")){
					this.allcollection=issue.getResult().occurrenceCount();
				}
				//all concepts
				if(issue.getIssueDescriptor().getId().equals("ac")){
					this.allconcepts=issue.getResult().occurrenceCount();
				}
				//all conceptschemes
				if(issue.getIssueDescriptor().getId().equals("cs")){
					this.allconceptscheme=issue.getResult().occurrenceCount();				
				}
			}
		}
		this.rulesFail=occurrenceCountFail;
		return resultList;
	}
}