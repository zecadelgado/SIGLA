package br.com.sigla.application.fiscal.port.out;

public interface FiscalStoragePort {

    void storeXml(String operationId, String xmlPayload);
}
