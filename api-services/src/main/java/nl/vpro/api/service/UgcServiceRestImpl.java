package nl.vpro.api.service;

import nl.vpro.api.rs.error.DataErrorException;
import nl.vpro.api.rs.error.NotFoundException;
import nl.vpro.api.rs.error.ServerErrorException;
import nl.vpro.api.rs.error.util.ErrorResponseToExceptionConverter;
import nl.vpro.api.transfer.ErrorResponse;
import nl.vpro.api.util.UrlProvider;
import nl.vpro.domain.ugc.annotation.Annotation;
import nl.vpro.domain.ugc.playerconfiguration.PlayerConfiguration;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Date: 24-4-12
 * Time: 13:12
 *
 * @author Ernst Bunders
 */
@Service("ugcService")
public class UgcServiceRestImpl implements UgcService {

    @Autowired
    private UrlProvider ugcUrlprovider;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ErrorResponseToExceptionConverter errorConverter;

    @Override
    public PlayerConfiguration getPlayerConfiguration(String id) {
        return getById("playerconfiguration", id, PlayerConfiguration.class);
    }

    @Override
    public PlayerConfiguration insertPlayerConfiguration(PlayerConfiguration playerConfiguration) {
        Object[] args = {ugcUrlprovider.getUrl()};
        try {
            ResponseEntity<PlayerConfiguration> responseEntity =
                restTemplate.postForEntity("{url}playerconfiguration.json", playerConfiguration, PlayerConfiguration.class, args);
            return responseEntity.getBody();
        } catch (HttpStatusCodeException e1) {
            throw errorConverter.convert(e1.getResponseBodyAsString(), MediaType.APPLICATION_JSON);
        } catch (ResourceAccessException e2) {
            throw new ServerErrorException("something went wrong upstream: " + e2.getMessage());
        }
    }


    @Override
    public PlayerConfiguration updatePlayerConfiguration(PlayerConfiguration playerConfiguration) {
        return null;
    }

    @Override
    public Annotation getAnnotation(String id) {
        return getById("annotation", id, Annotation.class);
    }

    @Override
    public Annotation getAnnotiationByPart(String id) {
        Object[] args = {ugcUrlprovider.getUrl(), id};
        try {
            ResponseEntity<Annotation> responseEntity = restTemplate.getForEntity("{url}annotation/bypart/{urn}.json", Annotation.class, args);
            return responseEntity.getBody();
        } catch (HttpClientErrorException cee) {
            throw new NotFoundException("Could not fetch annotation with id " + id + ". It does not exist");
        } catch (Exception e) {
            throw new ServerErrorException("Something went wrong fetching an annotation from the ugc service: " + e.getMessage(), e);
        }
    }



    private <T> T getById(String typeName, String id, Class<T> type) {
        Object[] args = {ugcUrlprovider.getUrl(), id};
        try {
            ResponseEntity<T> responseEntity = restTemplate.getForEntity("{url}" + typeName + "/{urn}.json", type, args);
            return responseEntity.getBody();
        } catch (HttpClientErrorException cee) {
            throw new NotFoundException("Could not fetch annotation with id " + id + ". It does not exist");
        } catch (Exception e) {
            throw new ServerErrorException("Something went wrong fetching an annotation from the ugc service: " + e.getMessage(), e);
        }
    }
}
