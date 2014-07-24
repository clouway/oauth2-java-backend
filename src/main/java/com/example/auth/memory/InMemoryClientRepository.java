package com.example.auth.memory;

import com.example.auth.core.client.Client;
import com.example.auth.core.client.ClientAuthentication;
import com.example.auth.core.client.ClientRepository;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.Map;

/**
 * @author Ivan Stefanov <ivan.stefanov@clouway.com>
 */
class InMemoryClientRepository implements ClientRepository, ClientAuthentication {
  private Map<String, Client> clients = Maps.newHashMap();


  @Inject
  public InMemoryClientRepository() {
  }

  @Override
  public void save(Client client) {
    clients.put(client.id, client);
  }

  @Override
  public Optional<Client> findById(String id) {
    return Optional.fromNullable(clients.get(id));
  }

  @Override
  public Boolean authenticate(String id, String secret) {
    if (clients.containsKey(id)) {
      if (clients.get(id).secret.equals(secret)) {
        return true;
      }
    }

    return false;
  }
}