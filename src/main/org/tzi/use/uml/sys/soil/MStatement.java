/*
 * USE - UML based specification environment
 * Copyright (C) 1999-2010 Mark Richters, University of Bremen
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

// $Id$

package org.tzi.use.uml.sys.soil;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.tzi.use.config.Options;
import org.tzi.use.parser.SrcPos;
import org.tzi.use.uml.mm.MAssociation;
import org.tzi.use.uml.mm.MAssociationClass;
import org.tzi.use.uml.mm.MAttribute;
import org.tzi.use.uml.mm.MClass;
import org.tzi.use.uml.mm.MOperation;
import org.tzi.use.uml.ocl.expr.Evaluator;
import org.tzi.use.uml.ocl.expr.Expression;
import org.tzi.use.uml.ocl.expr.MultiplicityViolationException;
import org.tzi.use.uml.ocl.value.ObjectValue;
import org.tzi.use.uml.ocl.value.StringValue;
import org.tzi.use.uml.ocl.value.Value;
import org.tzi.use.uml.sys.MLink;
import org.tzi.use.uml.sys.MLinkObject;
import org.tzi.use.uml.sys.MObject;
import org.tzi.use.uml.sys.MOperationCall;
import org.tzi.use.uml.sys.MSystemException;
import org.tzi.use.uml.sys.MSystemState.DeleteObjectResult;
import org.tzi.use.uml.sys.StatementEvaluationResult;
import org.tzi.use.uml.sys.events.AttributeAssignedEvent;
import org.tzi.use.uml.sys.events.LinkDeletedEvent;
import org.tzi.use.uml.sys.events.LinkInsertedEvent;
import org.tzi.use.uml.sys.events.ObjectCreatedEvent;
import org.tzi.use.uml.sys.events.ObjectDestroyedEvent;
import org.tzi.use.uml.sys.events.OperationEnteredEvent;
import org.tzi.use.uml.sys.events.OperationExitedEvent;
import org.tzi.use.uml.sys.ppcHandling.PPCHandler;
import org.tzi.use.util.StringUtil;
import org.tzi.use.util.soil.exceptions.EvaluationFailedException;


/**
 * Base class for all SOIL statements.
 * @author Daniel Gent
 *
 */
public abstract class MStatement {

	/**
	 * The source position of the statement (if specified).
	 */
	private SrcPos fSourcePosition;
	
	/** TODO */
	private boolean fIsOperationBody = false;
	
	private static final String SHELL_PREFIX = "!";
	
	/**
	 * TODO
	 * @return
	 */
	public boolean hasSourcePosition() {
		return fSourcePosition != null;
	}
	
	
	/**
	 * TODO
	 * @return
	 */
	public SrcPos getSourcePosition() {
		return fSourcePosition;
	}
	
	
	/**
	 * TODO
	 * @param sourcePosition
	 */
	public void setSourcePosition(SrcPos sourcePosition) {
		fSourcePosition = sourcePosition;
	}
	
	
	
	
	/**
	 * TODO
	 * @return
	 */
	public boolean isEmptyStatement() {
		return this == MEmptyStatement.getInstance();
	}
	
	
	/**
	 * TODO
	 * @return
	 */
	public boolean isOperationBody() {
		return fIsOperationBody;
	}


	/**
	 * TODO
	 * @param isOperationBody
	 */
	public void setIsOperationBody(boolean isOperationBody) {
		fIsOperationBody = isOperationBody;
	}
	
	
	/**
	 * Returns the shell command for the statement prefixed by
	 * the shell prefix {@link #SHELL_PREFIX}.
	 * @return The textual form of this statement for the USE shell.
	 */
	public final String getShellCommand() {
		return SHELL_PREFIX + shellCommand();
	}
	
	/**
	 * Returns the shell command of this statement without the shell prefix. 
	 * @return The command text of this statement.
	 */
	protected abstract String shellCommand();
	
	
	/**
	 * TODO
	 * @return
	 */
	public abstract boolean hasSideEffects();
	
	
	/**
	 * Returns true if this statement can used inside an
	 * OCL expression. This depends on the setting of the
	 * option  {@link Options#soilFromOCL} which can 
	 * be configured by a program argument.
	 * @return <code>true</code> if the statement is callable from an OCL expression with the current setting.
	 */
	public boolean isCallableInOCL() {
		switch (Options.soilFromOCL) {
		case ALL                  : return true;
		case SIDEEFFECT_FREE_ONLY : return !hasSideEffects();
		case NONE                 :
		default                   : return false;
		}	
	}
	
	
	@Override
	public abstract String toString();

	/**
	 * TODO
	 * @param indent
	 * @param indentIncr
	 * @return
	 */
	public String toVisitorString(int indent, int indentIncr) {
		
		return toVisitorString(
				new StringBuilder(StringUtil.repeat(" ", indent)), 
				StringUtil.repeat(" ", indentIncr));
		
	}
	
	
	/**
	 * TODO
	 * @return
	 */
	private String toVisitorString(
			StringBuilder indent, 
			String indentIncr) {
		
		StringBuilder result = new StringBuilder();
		
		toVisitorString(indent, indentIncr, result);
		
		return result.toString();
	}
	
	
	/**
	 * TODO
	 * @param indent
	 * @param indentIncrease
	 * @param target
	 */
	protected void toVisitorString(
			StringBuilder indent,
			String indentIncrease,
			StringBuilder target) {
		
		target.append(indent);
		target.append(shellCommand());
	}
	

	/**
	 * TODO
	 * @param context
	 * @param result
	 */
	public void evaluateGuarded(
			SoilEvaluationContext context,
			StatementEvaluationResult result) {
		
		try {
			evaluate(context, result);
		} catch (EvaluationFailedException e) {
			result.setException(e);
		}
	}


	/**
	 * TODO
	 * @param hasUndoStatement
	 * @throws EvaluationFailedException
	 */
	protected abstract void evaluate(SoilEvaluationContext context,
			StatementEvaluationResult result) throws EvaluationFailedException;


	/**
	 * TODO
	 * @param expression
	 * @param mustBeDefined
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected Value evaluateExpression(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			Expression expression, 
			boolean mustBeDefined) throws EvaluationFailedException {
		
		Evaluator evaluator = new Evaluator();
		
		Value value;
		
		context.enterExpression(expression);
		try {
			value = evaluator.eval(
					expression, 
					context.getState(), 
					context.getVarEnv().constructVarBindings());
		} catch (MultiplicityViolationException e) {
			throw new EvaluationFailedException(this,
					"Evaluation of expression "
							+ StringUtil.inQuotes(expression)
							+ " failed due to following reason:\n  "
							+ e.getMessage());
		} finally {
			context.exitExpression();
		}
		
		if (mustBeDefined && value.isUndefined()) {
			throw new EvaluationFailedException(this, "The value of expression " +
					StringUtil.inQuotes(expression) +
					" is undefined.");
		}
		
		return value;
	}
	
	
	/**
	 * TODO
	 * @param expression
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected Value evaluateExpression(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			Expression expression) throws EvaluationFailedException {
		
		return evaluateExpression(context, result, expression, false);
	}
	

	/**
	 * TODO
	 * @param expression
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected MObject evaluateObjectExpression(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			Expression expression) throws EvaluationFailedException {
		
		Value value = evaluateExpression(context, result, expression, true);
		
		if (value instanceof ObjectValue) {
			return ((ObjectValue)value).value();
		} else {
			throw new EvaluationFailedException(this, "Expression "
					+ StringUtil.inQuotes(expression)
					+ " is expected to evaluate to an object "
					+ ", but its type is "
					+ StringUtil.inQuotes(expression.type()) + ".");
		}
	}
		
	
	/**
	 * TODO
	 * @param expressions
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected List<MObject> evaluateObjectExpressions(			
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			List<Expression> expressions) throws EvaluationFailedException {
		
		List<MObject> vresult = new ArrayList<MObject>(expressions.size());
		
		for (Expression expression : expressions) {
			vresult.add(evaluateObjectExpression(context, result, expression));
		}
		
		return vresult;
	}
	
	/**
	 * TODO
	 * @param expression
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected String evaluateString(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			Expression expression) throws EvaluationFailedException {
		
		Value value = evaluateExpression(context, result, expression, true);
	
		if (value instanceof StringValue) {
			return ((StringValue)value).value();
		} else {
			throw new EvaluationFailedException(this, "Expression " + 
					StringUtil.inQuotes(expression) + 
					" is expected to be of type " +
					StringUtil.inQuotes("String") +
					", found " +
					StringUtil.inQuotes(expression.type()) +
					".");
		}
	}
	
	
	/**
	 * TODO
	 * @param statement
	 * @throws EvaluationFailedException
	 */
	public void evaluateSubStatement(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MStatement statement) throws EvaluationFailedException {
		
		statement.evaluateGuarded(context, result);
		
		if (result.getException() != null) {
			throw result.getException();
		}
	}
	
	
	/**
	 * TODO
	 * @param rValue
	 * @return
	 * @throws EvaluationFailedException 
	 */
	protected Value evaluateRValue(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MRValue rValue, 
			boolean mustBeDefined) throws EvaluationFailedException {
		
		Value value = rValue.evaluate(context, result, this);
		
		if (mustBeDefined && value.isUndefined()) {
			throw new EvaluationFailedException(this, "The value of rValue " +
					StringUtil.inQuotes(rValue) +
					" is undefined.");
		}
		
		return value;
	}
	
	
	/**
	 * TODO
	 * @param rValue
	 * @return
	 * @throws EvaluationFailedException 
	 */
	protected Value evaluateRValue(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MRValue rValue) throws EvaluationFailedException {
		
		return evaluateRValue(context, result, rValue, false);
	}
	
	
	/**
	 * TODO	
	 * @param rValue
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected MObject evaluateObjectRValue(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MRValue rValue) throws EvaluationFailedException {
		
		Value value = evaluateRValue(context, result, rValue, true);
		
		if (value instanceof ObjectValue) {
			return ((ObjectValue)value).value();
		} else {
			throw new EvaluationFailedException(this, "RValue " +
					StringUtil.inQuotes(rValue) +
					" is expected to evaluate to an object " +
					", but its type is " +
					StringUtil.inQuotes(rValue.getType()) +
					".");
		}
	}
	
	
	/**
	 * TODO
	 * @param rValues
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected List<MObject> evaluateObjectRValues(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			List<MRValue> rValues) throws EvaluationFailedException {
		
		List<MObject> vresult = new ArrayList<MObject>(rValues.size());
		
		for (MRValue rValue : rValues) {
			vresult.add(evaluateObjectRValue(context, result, rValue));
		}
		
		return vresult;
	}
	
	
	/**
	 * TODO
	 * @param variableName
	 * @param value
	 */
	protected void assignVariable(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			String variableName, Value value) {
		
		Value oldValue = context.getVarEnv().lookUp(variableName);
		
		if (oldValue != null) {
			result.prependToInverseStatement(
					new MVariableAssignmentStatement(
							variableName, 
							oldValue));
		} else {
			result.prependToInverseStatement(
					new MVariableDestructionStatement(variableName));
		}
			
		context.getVarEnv().assign(variableName, value);	
	}
	
	
	/**
	 * TODO
	 * @param objectClass
	 * @param objectName
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected MObject createObject(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MClass objectClass, 
			String objectName) throws EvaluationFailedException {
		
		MObject newObject;
		try {
			newObject = context.getState().createObject(objectClass, objectName);
		} catch (MSystemException e) {
			throw new EvaluationFailedException(this, e);
		}
		
		result.getStateDifference().addNewObject(newObject);
		
		result.prependToInverseStatement(
				new MObjectDestructionStatement(newObject.value()));
		
		result.appendEvent(new ObjectCreatedEvent(this, newObject));
		
		return newObject;
	}
	
	
	/**
	 * TODO
	 * @param associationClass
	 * @param linkObjectName
	 * @param participants
	 * @return
	 * @throws EvaluationFailedException
	 */
	protected MLinkObject createLinkObject(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MAssociationClass associationClass, 
			String linkObjectName, 
			List<MObject> participants,
			List<List<Value>> qualifierValues) throws EvaluationFailedException {
		
		MLinkObject newLinkObject;
		try {
			newLinkObject = 
				context.getState().createLinkObject(
					associationClass, 
					linkObjectName, 
					participants,
					qualifierValues);
			
		} catch (MSystemException e) {
			throw new EvaluationFailedException(this, e);
		}
		
		result.getStateDifference().addNewLinkObject(newLinkObject);
		
		result.prependToInverseStatement(
				new MObjectDestructionStatement(newLinkObject.value()));
		
		result.appendEvent(
				new LinkInsertedEvent(this, associationClass, participants));
		
		return newLinkObject;
	}
	
	
	/**
	 * TODO
	 * @param object
	 * @throws EvaluationFailedException 
	 */
	protected void destroyObject(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MObject object) throws EvaluationFailedException {
		
		// we cannot destroy an object with an active operation. we need to 
		// check whether this object, or any of the link objects possibly
		// connected to this object have an active operation
		Set<MObject> objectsAffectedByDeletion = 
			context.getState().getObjectsAffectedByDestruction(object);
		
		for (MObject affectedObject : objectsAffectedByDeletion) {
			if (context.getSystem().hasActiveOperation(affectedObject)) {
				throw new EvaluationFailedException(
						this,
						"Object "
								+ StringUtil.inQuotes(affectedObject)
								+ " has an active operation and thus cannot be deleted.");
			}
		}
		
		// .deleteObject() also takes care of the links this
		// object has been participating in
		DeleteObjectResult deleteResult = 
			context.getState().deleteObject(object);	
	    result.getStateDifference().addDeleteResult(deleteResult);
		
		Map<MObject, List<String>> undefinedTopLevelReferences = 
			new HashMap<MObject, List<String>>();
			
		for (MObject destroyedObject : deleteResult.getRemovedObjects()) {
			List<String> topLevelReferences = 
				context.getVarEnv().getTopLevelReferencesTo(destroyedObject);
			
			if (!topLevelReferences.isEmpty()) {
				undefinedTopLevelReferences.put(
						destroyedObject,
						topLevelReferences);
			}
					
			context.getVarEnv().undefineReferencesTo(destroyedObject);
		}
		
		result.prependToInverseStatement(
				new MObjectRestorationStatement(
						deleteResult, 
						undefinedTopLevelReferences));
		
		if (object instanceof MLink) {
			MLink link = (MLink)object;
			result.appendEvent(new LinkDeletedEvent(
					this,
					link.association(), 
					Arrays.asList(link.linkedObjectsAsArray())));
		} else {
			result.appendEvent(new ObjectDestroyedEvent(this, object));
		}
		
		Set<MLink> deletedLinks = 
			new HashSet<MLink>(deleteResult.getRemovedLinks());
		Set<MObject> deletedObjects = 
			new HashSet<MObject>(deleteResult.getRemovedObjects());
		
		deletedLinks.remove(object);
		deletedObjects.remove(object);
		
		for (MObject o : deletedObjects) {
			if (o instanceof MLink) {
				deletedLinks.add((MLink)o);
			}
		}
		
		deletedObjects.removeAll(deletedLinks);
		
		for (MLink l : deletedLinks) {
			result.appendEvent(new LinkDeletedEvent(
					this,
					l.association(), 
					Arrays.asList(l.linkedObjectsAsArray())));
		}
		
		for (MObject o : deletedObjects) {
			result.appendEvent(new ObjectDestroyedEvent(this, o));
		}
	}
	
	
	/**
	 * TODO
	 * @param object
	 * @param attribute
	 * @param value
	 * @throws EvaluationFailedException
	 */
	protected void assignAttribute(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MObject object, 
			MAttribute attribute, 
			Value value) throws EvaluationFailedException {
		
		Value oldValue;
		
		try {
			oldValue = object.state(context.getState()).attributeValue(attribute);
			object.state(context.getState()).setAttributeValue(attribute, value);
		} catch (IllegalArgumentException e) {
			throw new EvaluationFailedException(this, e);
		}
		
		result.getStateDifference().addModifiedObject(object);
		
		result.prependToInverseStatement(
				new MAttributeAssignmentStatement(
						object, 
						attribute, 
						oldValue));
		
		result.appendEvent(
				new AttributeAssignedEvent(
						this, 
						object, 
						attribute, 
						value));
	}
	
	
	/**
	 * TODO
	 * @param association
	 * @param participants
	 * @param qualifierValues
	 * @throws EvaluationFailedException 
	 */
	protected MLink insertLink(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MAssociation association, 
			List<MObject> participants,
			List<List<Value>> qualifierValues) throws EvaluationFailedException {
		
		MLink newLink;
		try {
			newLink = context.getState().createLink(association, participants, qualifierValues);
		} catch (MSystemException e) {
			throw new EvaluationFailedException(this, e);
		}
		
		result.getStateDifference().addNewLink(newLink);
		
		List<MRValue> wrappedParticipants = 
			new ArrayList<MRValue>(participants.size());
		
		for (MObject participant : participants) {
			wrappedParticipants.add(
					new MRValueExpression(participant));
		}
		
		List<List<MRValue>> wrappedQualifier = new LinkedList<List<MRValue>>();
		
		for(List<Value> qValues : qualifierValues) {
			List<MRValue> wrappedQValues;
			if (qValues.size() == 0) {
				wrappedQValues = Collections.emptyList();
			} else {
				wrappedQValues = new LinkedList<MRValue>();
				for (Value qValue : qValues) {
					wrappedQValues.add(new MRValueExpression(qValue));
				}
			}
			wrappedQualifier.add(wrappedQValues);
		}
		
		result.prependToInverseStatement(
				new MLinkDeletionStatement(association, wrappedParticipants, wrappedQualifier));
		
		result.appendEvent(
				new LinkInsertedEvent(
						this, 
						association, 
						participants));
		
		return newLink;
	}
	
	
	/**
	 * TODO
	 * @param association
	 * @param participants
	 * @throws EvaluationFailedException
	 */
	protected void deleteLink(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MAssociation association, 
			List<MObject> participants,
			List<List<Value>> qualifierValues) throws EvaluationFailedException {
		
		// we need to find out if this is actually a link object, since we need
		// to call destroyObject in that case to get the correct undo 
		// statement
		MLink link = context.getState().linkBetweenObjects(association, participants, qualifierValues);
		
		if ((link != null) && (link instanceof MLinkObject)) {
			destroyObject(context, result, (MLinkObject)link);
			return;
		}
		
		try {
			result.getStateDifference().addDeleteResult(
					context.getState().deleteLink(association, participants, qualifierValues));
		} catch (MSystemException e) {
			throw new EvaluationFailedException(this, e);
		}
		
		List<MRValue> wrappedParticipants = 
			new ArrayList<MRValue>(participants.size());
		
		for (MObject participant : participants) {
			wrappedParticipants.add(
					new MRValueExpression(participant));
		}
		
		List<List<MRValue>> wrappedQualifier;
		if (qualifierValues == null || qualifierValues.isEmpty()) {
			wrappedQualifier = Collections.emptyList(); 
		} else {
			wrappedQualifier = new ArrayList<List<MRValue>>(qualifierValues.size());
		
			for (List<Value> endQualifier : qualifierValues) {
				List<MRValue> endQualifierValues;
				
				if (endQualifier == null || endQualifier.isEmpty()) {
					endQualifierValues = Collections.emptyList();
				} else {
					endQualifierValues = new ArrayList<MRValue>();
					for (Value v : endQualifier) {
						endQualifierValues.add(new MRValueExpression(v));
					}
				}
				
				wrappedQualifier.add(endQualifierValues);
			}
		}
		result.prependToInverseStatement(
				new MLinkInsertionStatement(association, wrappedParticipants, wrappedQualifier));
		
		result.appendEvent(
				new LinkDeletedEvent(
						this, 
						association, 
						participants));
	}
	
	
	/**
	 * TODO
	 * @param operationCall
	 */
	public void enteredOperationDuringEvaluation(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MOperationCall operationCall) {
		
		result.appendEvent(
				new OperationEnteredEvent(
						this, 
						operationCall));
	}
	
	
	/**
	 * TODO
	 * @param operationCall
	 */
	public void exitedOperationDuringEvaluation(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MOperationCall operationCall) {
		
		result.appendEvent(
				new OperationExitedEvent(this, operationCall));
	}
	
	
	/**
	 * TODO
	 * @param self
	 * @param operation
	 * @param arguments
	 * @throws EvaluationFailedException
	 */
	protected MOperationCall enterOperation(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			MObject self, 
			MOperation operation, 
			Value[] arguments,
			PPCHandler preferredPPCHandler,
			boolean isOpenter) throws EvaluationFailedException {
	
		MOperationCall operationCall = 
			new MOperationCall(this, self, operation, arguments);
		
		operationCall.setPreferredPPCHandler(preferredPPCHandler);
		
		try {
			context.getSystem().enterNonQueryOperation(context, result, operationCall, isOpenter);
		} catch (MSystemException e) {
			throw new EvaluationFailedException(this, e);
		}
		
		return operationCall;
	}
	
	
	/**
	 * TODO
	 * @param resultValue
	 * @throws EvaluationFailedException
	 */
	protected MOperationCall exitOperation(
			SoilEvaluationContext context,
			StatementEvaluationResult result,
			Value resultValue,
			PPCHandler preferredPPCHandler) throws EvaluationFailedException {
		
		MOperationCall currentOperation = context.getSystem().getCurrentOperation();
		
		if (currentOperation == null) {
			throw new EvaluationFailedException(this, "No current operation");
		}
		
		if (preferredPPCHandler != null) {
			currentOperation.setPreferredPPCHandler(preferredPPCHandler);
		}
		
		try {
			context.getSystem().exitNonQueryOperation(context,result,resultValue);
		} catch (MSystemException e) {
			throw new EvaluationFailedException(this, e);
		}
		
		return currentOperation;
	}
	
	
	
}