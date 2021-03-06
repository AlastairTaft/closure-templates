/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.template.soy.soytree;

import com.google.common.collect.ImmutableList;
import com.google.template.soy.base.SourceLocation;
import com.google.template.soy.exprtree.ExprRootNode;
import com.google.template.soy.soyparse.ErrorReporter;
import com.google.template.soy.soyparse.ErrorReporter.Checkpoint;
import com.google.template.soy.soyparse.SoyError;
import com.google.template.soy.soyparse.TransitionalThrowingErrorReporter;
import com.google.template.soy.soytree.SoyNode.ExprHolderNode;

import java.util.List;


/**
 * Node representing a 'let' statement with a value expression.
 *
 * <p> Important: Do not use outside of Soy code (treat as superpackage-private).
 *
 */
public final class LetValueNode extends LetNode implements ExprHolderNode {

  private static final SoyError SELF_ENDING_WITHOUT_VALUE
      = SoyError.of("A ''let'' tag should be self-ending (with a trailing ''/'') if and only if "
          + "it also contains a value (invalid tag is '{'let {0} /'}').");
  private static final SoyError KIND_ATTRIBUTE_NOT_ALLOWED_WITH_VALUE
      = SoyError.of("The ''kind'' attribute is not allowed on self-ending ''let'' tags that "
          + "contain a value (invalid tag is '{'let {0} /'}').");


  /** The value expression that the variable is set to. */
  private final ExprRootNode<?> valueExpr;


  private LetValueNode(int id, String localVarName, String commandText, ExprRootNode<?> valueExpr) {
    super(id, localVarName, commandText);
    this.valueExpr = valueExpr;
  }


  /**
   * Copy constructor.
   * @param orig The node to copy.
   */
  private LetValueNode(LetValueNode orig) {
    super(orig);
    this.valueExpr = orig.valueExpr.clone();
  }


  @Override public Kind getKind() {
    return Kind.LET_VALUE_NODE;
  }


  /**
   * Return The local variable name (without preceding '$').
   */
  @Override public final String getVarName() {
    return var.name();
  }


  /**
   * Returns the value expression that the variable is set to.
   */
  public ExprRootNode<?> getValueExpr() {
    return valueExpr;
  }


  @Override public List<ExprUnion> getAllExprUnions() {
    return ImmutableList.of(new ExprUnion(valueExpr));
  }


  @Override public LetValueNode clone() {
    return new LetValueNode(this);
  }

  /**
   * Builder for {@link LetValueNode}.
   */
  public static final class Builder {
    public static final LetValueNode ERROR = new Builder(-1, "$error: 1", SourceLocation.UNKNOWN)
        .buildAndThrowIfInvalid(); // guaranteed to be valid

    private final int id;
    private final String commandText;
    private final SourceLocation sourceLocation;

    /**
     * @param id The node's id.
     * @param commandText The node's command text.
     * @param sourceLocation The node's source location.
     */
    public Builder(int id, String commandText, SourceLocation sourceLocation) {
      this.id = id;
      this.commandText = commandText;
      this.sourceLocation = sourceLocation;
    }

    /**
     * Returns a new {@link LetValueNode} built from the builder's state. If the builder's state
     * is invalid, errors are reported to the {@code errorManager} and {Builder#ERROR} is returned.
     */
    public LetValueNode build(ErrorReporter errorReporter) {
      Checkpoint checkpoint = errorReporter.checkpoint();
      CommandTextParseResult parseResult
          = parseCommandTextHelper(commandText, errorReporter, sourceLocation);

      if (parseResult.valueExpr == null) {
        errorReporter.report(sourceLocation, SELF_ENDING_WITHOUT_VALUE, commandText);
      }

      if (parseResult.contentKind != null) {
        errorReporter.report(sourceLocation, KIND_ATTRIBUTE_NOT_ALLOWED_WITH_VALUE, commandText);
      }

      if (errorReporter.errorsSince(checkpoint)) {
        return ERROR;
      }

      LetValueNode node
          = new LetValueNode(id, parseResult.localVarName, commandText, parseResult.valueExpr);
      node.setSourceLocation(sourceLocation);
      return node;
    }

    private LetValueNode buildAndThrowIfInvalid() {
      TransitionalThrowingErrorReporter errorReporter = new TransitionalThrowingErrorReporter();
      LetValueNode node = build(errorReporter);
      errorReporter.throwIfErrorsPresent();
      return node;
    }
  }

}
