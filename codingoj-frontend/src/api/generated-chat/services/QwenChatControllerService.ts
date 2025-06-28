/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { ChatAIRequest } from '../models/ChatAIRequest';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class QwenChatControllerService {

    /**
     * @param chatAiRequest 
     * @returns string OK
     * @throws ApiError
     */
    public static doChat(
chatAiRequest: ChatAIRequest,
): CancelablePromise<string> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/chat/qwen',
            query: {
                'chatAIRequest': chatAiRequest,
            },
        });
    }

}
