package br.com.sigla.aplicacao.clientes.casodeuso;

import br.com.sigla.aplicacao.clientes.porta.entrada.CasoDeUsoCliente;
import br.com.sigla.aplicacao.clientes.porta.saida.RepositorioCliente;
import br.com.sigla.dominio.clientes.Cliente;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class CasoDeUsoGerenciarCliente implements CasoDeUsoCliente {

    private static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
    );

    private final RepositorioCliente repository;

    public CasoDeUsoGerenciarCliente(RepositorioCliente repository) {
        this.repository = repository;
    }

    @Override
    public void register(RegisterClienteCommand command) {
        Cliente customer = toCliente(command);
        validar(customer, true);
        repository.save(customer);
    }

    @Override
    public void update(RegisterClienteCommand command) {
        Cliente customer = toCliente(command);
        repository.findById(customer.id())
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        validar(customer, false);
        repository.save(customer);
    }

    @Override
    public void inativar(String id) {
        Cliente atual = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        repository.save(withAtivo(atual, false));
    }

    @Override
    public void reativar(String id) {
        Cliente atual = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        validar(atual, false);
        repository.save(withAtivo(atual, true));
    }

    @Override
    public void excluirFisicamente(String id) {
        repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente nao encontrado."));
        if (repository.hasLinkedRecords(id)) {
            throw new IllegalArgumentException("Nao e possivel excluir fisicamente: ha ordens, financeiro, indicacoes ou outros vinculos. Use inativacao.");
        }
        repository.deleteById(id);
    }

    @Override
    public List<Cliente> listAll() {
        return repository.findAll();
    }

    @Override
    public List<Cliente> filtrar(FiltroCliente filtro) {
        String termo = normalize(filtro == null ? "" : filtro.texto()).toLowerCase(Locale.ROOT);
        Boolean ativo = filtro == null ? null : filtro.ativo();
        Cliente.TipoCliente tipo = filtro == null ? null : filtro.tipo();
        return repository.findAll().stream()
                .filter(cliente -> ativo == null || cliente.ativo() == ativo)
                .filter(cliente -> tipo == null || cliente.tipo() == tipo)
                .filter(cliente -> termo.isBlank()
                        || cliente.name().toLowerCase(Locale.ROOT).contains(termo)
                        || cliente.razaoSocial().toLowerCase(Locale.ROOT).contains(termo)
                        || cliente.nomeFantasia().toLowerCase(Locale.ROOT).contains(termo)
                        || onlyDigits(cliente.cpf()).contains(onlyDigits(termo))
                        || onlyDigits(cliente.cnpj()).contains(onlyDigits(termo))
                        || onlyDigits(cliente.phone()).contains(onlyDigits(termo))
                        || cliente.email().toLowerCase(Locale.ROOT).contains(termo))
                .toList();
    }

    private Cliente toCliente(RegisterClienteCommand command) {
        Objects.requireNonNull(command, "command is required");
        List<Cliente.ContactPerson> contacts = command.contacts() == null ? List.of() : command.contacts().stream()
                .map(contact -> new Cliente.ContactPerson(contact.id(), contact.name(), contact.role(), contact.phone(), contact.email(), contact.principal()))
                .toList();
        contacts = normalizarResponsaveis(contacts);
        return new Cliente(
                command.id(),
                command.tipo(),
                command.name(),
                command.razaoSocial(),
                command.nomeFantasia(),
                command.cpf(),
                command.cnpj(),
                command.phone(),
                command.email(),
                command.cep(),
                command.rua(),
                command.numero(),
                command.complemento(),
                command.bairro(),
                command.cidade(),
                command.estado(),
                contacts,
                command.notes(),
                command.ativo()
        );
    }

    private void validar(Cliente cliente, boolean novo) {
        if (cliente.tipo() == Cliente.TipoCliente.PESSOA_FISICA) {
            require(cliente.name(), "Informe o nome da pessoa fisica.");
            require(cliente.cpf(), "Informe o CPF da pessoa fisica.");
            validarCpf(cliente.cpf());
            if (repository.existsActiveCpf(onlyDigits(cliente.cpf()), cliente.id())) {
                throw new IllegalArgumentException("CPF ja cadastrado em outro cliente ativo.");
            }
        } else {
            require(cliente.razaoSocial(), "Informe a razao social da pessoa juridica.");
            require(cliente.cnpj(), "Informe o CNPJ da pessoa juridica.");
            validarCnpj(cliente.cnpj());
            if (repository.existsActiveCnpj(onlyDigits(cliente.cnpj()), cliente.id())) {
                throw new IllegalArgumentException("CNPJ ja cadastrado em outro cliente ativo.");
            }
        }
        if (!cliente.email().isBlank()) {
            validarEmail(cliente.email());
            if (repository.existsActiveEmail(cliente.email(), cliente.id())) {
                throw new IllegalArgumentException("E-mail ja cadastrado em outro cliente ativo.");
            }
        }
        if (!cliente.phone().isBlank()) {
            validarTelefone(cliente.phone(), "Telefone principal invalido.");
        }
        validarEndereco(cliente);
        for (Cliente.ContactPerson responsavel : cliente.contacts()) {
            require(responsavel.name(), "Informe o nome do responsavel.");
            if (!responsavel.phone().isBlank()) {
                validarTelefone(responsavel.phone(), "Telefone do responsavel invalido.");
            }
            if (!responsavel.email().isBlank()) {
                validarEmail(responsavel.email());
            }
        }
    }

    private List<Cliente.ContactPerson> normalizarResponsaveis(List<Cliente.ContactPerson> contacts) {
        if (contacts.isEmpty()) {
            return contacts;
        }
        boolean temPrincipal = contacts.stream().anyMatch(Cliente.ContactPerson::principal);
        java.util.ArrayList<Cliente.ContactPerson> normalizados = new java.util.ArrayList<>();
        boolean principalEncontrado = false;
        for (int i = 0; i < contacts.size(); i++) {
            Cliente.ContactPerson contact = contacts.get(i);
            boolean principal = (temPrincipal ? contact.principal() && !principalEncontrado : i == 0);
            if (principal) {
                principalEncontrado = true;
            }
            normalizados.add(new Cliente.ContactPerson(contact.id(), contact.name(), contact.role(), contact.phone(), contact.email(), principal));
        }
        return List.copyOf(normalizados);
    }

    private Cliente withAtivo(Cliente cliente, boolean ativo) {
        return new Cliente(
                cliente.id(),
                cliente.tipo(),
                cliente.name(),
                cliente.razaoSocial(),
                cliente.nomeFantasia(),
                cliente.cpf(),
                cliente.cnpj(),
                cliente.phone(),
                cliente.email(),
                cliente.cep(),
                cliente.rua(),
                cliente.numero(),
                cliente.complemento(),
                cliente.bairro(),
                cliente.cidade(),
                cliente.estado(),
                cliente.contacts(),
                cliente.notes(),
                ativo
        );
    }

    private void validarEndereco(Cliente cliente) {
        if (!cliente.cep().isBlank() && !onlyDigits(cliente.cep()).matches("\\d{8}")) {
            throw new IllegalArgumentException("CEP invalido.");
        }
        if (!cliente.estado().isBlank()) {
            String uf = cliente.estado().trim().toUpperCase(Locale.ROOT);
            if (uf.length() != 2 || !UFS.contains(uf)) {
                throw new IllegalArgumentException("UF invalida.");
            }
        }
        boolean algumEndereco = !cliente.rua().isBlank()
                || !cliente.numero().isBlank()
                || !cliente.bairro().isBlank()
                || !cliente.cidade().isBlank()
                || !cliente.estado().isBlank()
                || !cliente.cep().isBlank();
        if (algumEndereco && (cliente.rua().isBlank()
                || cliente.numero().isBlank()
                || cliente.bairro().isBlank()
                || cliente.cidade().isBlank()
                || cliente.estado().isBlank())) {
            throw new IllegalArgumentException("Preencha rua, numero, bairro, cidade e UF quando informar endereco.");
        }
    }

    private void validarCpf(String cpf) {
        String digits = onlyDigits(cpf);
        if (digits.length() != 11 || digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CPF invalido.");
        }
        int d1 = digitoCpf(digits, 9);
        int d2 = digitoCpf(digits, 10);
        if (digits.charAt(9) - '0' != d1 || digits.charAt(10) - '0' != d2) {
            throw new IllegalArgumentException("CPF invalido.");
        }
    }

    private int digitoCpf(String digits, int length) {
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * (length + 1 - i);
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private void validarCnpj(String cnpj) {
        String digits = onlyDigits(cnpj);
        if (digits.length() != 14 || digits.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("CNPJ invalido.");
        }
        int d1 = digitoCnpj(digits, 12);
        int d2 = digitoCnpj(digits, 13);
        if (digits.charAt(12) - '0' != d1 || digits.charAt(13) - '0' != d2) {
            throw new IllegalArgumentException("CNPJ invalido.");
        }
    }

    private int digitoCnpj(String digits, int length) {
        int[] weights = length == 12
                ? new int[]{5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[]{6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (digits.charAt(i) - '0') * weights[i];
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }

    private void validarEmail(String email) {
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("E-mail invalido.");
        }
    }

    private void validarTelefone(String telefone, String message) {
        String digits = onlyDigits(telefone);
        if (digits.length() < 10 || digits.length() > 13) {
            throw new IllegalArgumentException(message);
        }
    }

    private void require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }
}

