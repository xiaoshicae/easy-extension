package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;

import java.util.List;

public interface ISession {

    /**
     *
     * @param code matched business or ability code
     * @param priority priority of matched business or ability code
     * @throws SessionException business null exception
     *
     */
    void setMatchedCode(String code, Integer priority) throws SessionException;

    /**
     *
     * @return matched business or ability code sorted by priority
     * @throws SessionException business null exception
     *
     */
    List<String> getMatchedCodes() throws SessionException;

    /**
     *
     * clear after session finish
     *
     */
    void remove();
}
