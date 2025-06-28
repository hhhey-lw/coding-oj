/* generated using openapi-typescript-codegen -- do no edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseResponseString } from '../models/BaseResponseString';
import type { UserResumeInfoDTO } from '../models/UserResumeInfoDTO';

import type { CancelablePromise } from '../core/CancelablePromise';
import { OpenAPI } from '../core/OpenAPI';
import { request as __request } from '../core/request';

export class ResumeControllerService {

    /**
     * @param userResumeInfoDto 
     * @returns BaseResponseString OK
     * @throws ApiError
     */
    public static startUploadInfo(
userResumeInfoDto: UserResumeInfoDTO,
): CancelablePromise<BaseResponseString> {
        return __request(OpenAPI, {
            method: 'POST',
            url: '/resume/start/upload/info',
            query: {
                'userResumeInfoDTO': userResumeInfoDto,
            },
        });
    }

    /**
     * @param resumeFileUrl 
     * @returns BaseResponseString OK
     * @throws ApiError
     */
    public static recognizeResume(
resumeFileUrl: string,
): CancelablePromise<BaseResponseString> {
        return __request(OpenAPI, {
            method: 'GET',
            url: '/resume/recognize/pdf',
            query: {
                'resumeFileUrl': resumeFileUrl,
            },
        });
    }

}
