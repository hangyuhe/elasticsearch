/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.core.security.authc.support;

import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.ToXContentObject;
import org.elasticsearch.common.xcontent.XContentBuilder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The result of attempting to invalidate one or multiple tokens. The result contains information about:
 * <ul>
 * <li>how many of the tokens were actually invalidated</li>
 * <li>how many tokens are not invalidated in this request because they were already invalidated</li>
 * <li>how many errors were encountered while invalidating tokens and the error details</li>
 * </ul>
 */
public class TokensInvalidationResult implements ToXContentObject, Writeable {

    private final List<String> invalidatedTokens;
    private final List<String> previouslyInvalidatedTokens;
    private final List<ElasticsearchException> errors;
    private final int attemptCount;

    public TokensInvalidationResult(List<String> invalidatedTokens, List<String> previouslyInvalidatedTokens,
                                    @Nullable List<ElasticsearchException> errors, int attemptCount) {
        Objects.requireNonNull(invalidatedTokens, "invalidated_tokens must be provided");
        this.invalidatedTokens = invalidatedTokens;
        Objects.requireNonNull(previouslyInvalidatedTokens, "previously_invalidated_tokens must be provided");
        this.previouslyInvalidatedTokens = previouslyInvalidatedTokens;
        if (null != errors) {
            this.errors = errors;
        } else {
            this.errors = Collections.emptyList();
        }
        this.attemptCount = attemptCount;
    }

    public TokensInvalidationResult(StreamInput in) throws IOException {
        this.invalidatedTokens = in.readList(StreamInput::readString);
        this.previouslyInvalidatedTokens = in.readList(StreamInput::readString);
        this.errors = in.readList(StreamInput::readException);
        this.attemptCount = in.readVInt();
    }

    public static TokensInvalidationResult emptyResult() {
        return new TokensInvalidationResult(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), 0);
    }


    public List<String> getInvalidatedTokens() {
        return invalidatedTokens;
    }

    public List<String> getPreviouslyInvalidatedTokens() {
        return previouslyInvalidatedTokens;
    }

    public List<ElasticsearchException> getErrors() {
        return errors;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject()
            .field("invalidated_tokens", invalidatedTokens.size())
            .field("previously_invalidated_tokens", previouslyInvalidatedTokens.size())
            .field("error_count", errors.size());
        if (errors.isEmpty() == false) {
            builder.field("error_details");
            builder.startArray();
            for (ElasticsearchException e : errors) {
                builder.startObject();
                ElasticsearchException.generateThrowableXContent(builder, params, e);
                builder.endObject();
            }
            builder.endArray();
        }
        return builder.endObject();
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        out.writeStringList(invalidatedTokens);
        out.writeStringList(previouslyInvalidatedTokens);
        out.writeCollection(errors, StreamOutput::writeException);
        out.writeVInt(attemptCount);
    }
}
