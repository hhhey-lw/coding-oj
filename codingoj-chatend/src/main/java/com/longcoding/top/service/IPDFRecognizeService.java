package com.longcoding.top.service;

public interface IPDFRecognizeService {

    /**
     * 识别PDF文件中的文本内容
     *
     * @param fileURL PDF文件的网络路径
     * @return 识别出的文本内容
     */
    String recognizeTextFromPDF(String fileURL);

}
