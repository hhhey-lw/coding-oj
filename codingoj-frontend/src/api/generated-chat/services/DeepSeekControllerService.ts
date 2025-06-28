/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ChatAIRequest } from '../models/ChatAIRequest';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class DeepSeekControllerService {

    /**
     * @param requestBody 
     * @returns string OK
     * @throws ApiError
     */
    public static streamChat(
requestBody: ChatAIRequest,
): CancelablePromise<Array<string>> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/chat',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

}
