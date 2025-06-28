/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseResponseString } from '../models/BaseResponseString';
import type { UploadFileRequest } from '../models/UploadFileRequest';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class FileControllerService {

    /**
     * @param requestBody 
     * @returns BaseResponseString OK
     * @throws ApiError
     */
    public static uploadSpeechFile(
requestBody?: {
file: Blob;
},
): CancelablePromise<BaseResponseString> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/file/upload/speech',
            body: requestBody,
            mediaType: 'application/json',
        });
    }

    /**
     * @param uploadFileRequest 
     * @param requestBody 
     * @returns BaseResponseString OK
     * @throws ApiError
     */
    public static uploadFile(
uploadFileRequest: UploadFileRequest,
requestBody?: {
file: Blob;
},
): CancelablePromise<BaseResponseString> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/file/upload/resume/pdf',
            query: {
                'uploadFileRequest': uploadFileRequest,
            },
            body: requestBody,
            mediaType: 'application/json',
        });
    }

}
