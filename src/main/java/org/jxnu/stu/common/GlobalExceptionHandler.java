package org.jxnu.stu.common;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

//    @ResponseStatus(HttpStatus.OK)
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public <T> ServerResponse<T> exceptionHandler(HttpServletRequest request, HttpServletResponse response,Exception e){
        if(e instanceof  BusinessException){
            BusinessException exception = (BusinessException) e;
            return ServerResponse.createServerResponse(exception.getCode(),exception.getMsg());
        }else if(e instanceof NoHandlerFoundException) {
            return ServerResponse.createServerResponse(ReturnCode.ERROR.getCode(),"没有找到对应的路径");
        }else {
            return ServerResponse.createServerResponse(ReturnCode.ERROR.getCode(),ReturnCode.ERROR.getMsg());
        }
    }
}
