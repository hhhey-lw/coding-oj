/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseResponseString } from '../models/BaseResponseString';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class TestControllerService {

    /**
     * @returns BaseResponseString OK
     * @throws ApiError
     */
    public static getToken(): CancelablePromise<BaseResponseString> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/test/token',
        });
    }

    /**
     * @returns BaseResponseString OK
     * @throws ApiError
     */
    public static ping(): CancelablePromise<BaseResponseString> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/test/ping',
        });
    }

}
