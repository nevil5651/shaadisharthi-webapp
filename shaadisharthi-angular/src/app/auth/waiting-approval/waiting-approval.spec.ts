import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { ToastrModule } from 'ngx-toastr';

import { WaitingApproval } from './waiting-approval';

describe('WaitingApproval', () => {
  let component: WaitingApproval;
  let fixture: ComponentFixture<WaitingApproval>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        WaitingApproval,
        HttpClientTestingModule,
        RouterTestingModule,
        ToastrModule.forRoot()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(WaitingApproval);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});