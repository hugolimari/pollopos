package com.example.pollogithub.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.pollogithub.data.entity.TurnoEntity;
import com.example.pollogithub.data.entity.UsuarioEntity;
import com.example.pollogithub.data.repository.PosRepository;

public class LoginViewModel extends AndroidViewModel {

    private final PosRepository repository;
    private final MutableLiveData<UsuarioEntity> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> loginError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    public LoginViewModel(@NonNull Application application) {
        super(application);
        repository = PosRepository.getInstance(application);
    }

    public LiveData<UsuarioEntity> getLoginSuccess() { return loginSuccess; }
    public LiveData<String> getLoginError() { return loginError; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public void login(String userOrPin, String password) {
        isLoading.setValue(true);
        repository.login(userOrPin, password, new PosRepository.Callback<UsuarioEntity>() {
            @Override
            public void onSuccess(UsuarioEntity usuario) {
                // Ensure shift exists or open one
                repository.abrirTurno(100.00, usuario.getId(), usuario.getSucursalId(), new PosRepository.Callback<TurnoEntity>() {
                    @Override
                    public void onSuccess(TurnoEntity turno) {
                        repository.getSessionManager().saveSession(
                                usuario.getId(),
                                usuario.getNombreCompleto(),
                                usuario.getRolId() == 1 ? "admin" : "cajero",
                                turno.getId(),
                                usuario.getSucursalId()
                        );
                        isLoading.setValue(false);
                        loginSuccess.setValue(usuario);
                    }

                    @Override
                    public void onError(String error) {
                        isLoading.setValue(false);
                        loginError.setValue("Error al iniciar turno: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                isLoading.setValue(false);
                loginError.setValue(error);
            }
        });
    }
}
