/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseResponseString } from '../models/BaseResponseString';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class SpeechControllerService {

    /**
     * @param audioUrl 
     * @returns BaseResponseString OK
     * @throws ApiError
     */
    public static recognizeSpeech(
audioUrl: string,
): CancelablePromise<BaseResponseString> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/speech/recognize',
            query: {
                'audioUrl': audioUrl,
            },
        });
    }

    /**
     * @param text 
     * @param voice 
     * @returns string OK
     * @throws ApiError
     */
    public static generateSpeech(
text: string,
voice?: string,
): CancelablePromise<string> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/speech/gen',
            query: {
                'text': text,
                'voice': voice,
            },
        });
    }

}
