package io.github.xiaoshicae.extension.core.session;

import io.github.xiaoshicae.extension.core.exception.SessionException;
import io.github.xiaoshicae.extension.core.exception.SessionNotFoundException;
import io.github.xiaoshicae.extension.core.exception.SessionParamException;

import java.util.List;

public interface ISession {

    /**
     * Set matched code with priority into session.
     *
     * @param code     code of matched business or ability instance
     * @param priority priority of matched business or ability instance
     * @throws SessionParamException if {@code code} is null or {@code priority} is null
     */
    void setMatchedCode(String code, Integer priority) throws SessionException;

    /**
     * Get matched codes from session.
     *
     * @return code of matched business or ability instance
     * @throws SessionNotFoundException matched code not found
     */
    List<String> getMatchedCodes() throws SessionException;

    /**
     * Clear session.
     */
    void remove();
}
